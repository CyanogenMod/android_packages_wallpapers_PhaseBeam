package com.android.phasebeam;

import static android.renderscript.Sampler.Value.NEAREST;
import static android.renderscript.Sampler.Value.WRAP;

import android.content.res.Resources;
import android.renderscript.Allocation;
import android.renderscript.Matrix4f;
import android.renderscript.Mesh;
import android.renderscript.Program;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramRaster;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.RenderScriptGL;
import android.renderscript.Sampler;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;

public class PhaseBeamRS {

    public static final int DOT_COUNT = 18;
    private Resources mRes;
    private RenderScriptGL mRS;
    private ScriptC_phasebeam mScript;
    int mHeight;
    int mWidth;

    private ScriptField_VpConsts mPvConsts;
    private Allocation dotAllocation;
    private Allocation beamAllocation;
    private Allocation bgAllocation;

    private ScriptField_Particle dotParticles;
    private Mesh dotMesh;

    private ScriptField_Particle beamParticles;
    private Mesh beamMesh;

    private int densityDPI;

    boolean inited = false;

    public void init(int dpi, RenderScriptGL rs, Resources res, int width, int height) {
        if (!inited) {
            densityDPI = dpi;

            mRS = rs;
            mRes = res;

            mWidth = width;
            mHeight = height;

            dotParticles = new ScriptField_Particle(mRS, DOT_COUNT);
            Mesh.AllocationBuilder smb2 = new Mesh.AllocationBuilder(mRS);
            smb2.addVertexAllocation(dotParticles.getAllocation());
            smb2.addIndexSetType(Mesh.Primitive.POINT);
            dotMesh = smb2.create();

            beamParticles = new ScriptField_Particle(mRS, DOT_COUNT);
            Mesh.AllocationBuilder smb3 = new Mesh.AllocationBuilder(mRS);
            smb3.addVertexAllocation(beamParticles.getAllocation());
            smb3.addIndexSetType(Mesh.Primitive.POINT);
            beamMesh = smb3.create();

            mScript = new ScriptC_phasebeam(mRS, mRes, R.raw.phasebeam);
            mScript.set_dotMesh(dotMesh);
            mScript.set_beamMesh(beamMesh);
            mScript.bind_dotParticles(dotParticles);
            mScript.bind_beamParticles(beamParticles);

            mPvConsts = new ScriptField_VpConsts(mRS, 1);

            createProgramVertex();
            createProgramRaster();
            createProgramFragmentStore();
            createProgramFragment();
            loadTextures();

            mScript.set_densityDPI(densityDPI);

            mRS.bindRootScript(mScript);

            mScript.invoke_positionParticles();
            inited = true;
        }
    }

    private Matrix4f getProjectionNormalized(int w, int h) {
        // range -1,1 in the narrow axis at z = 0.
        Matrix4f m1 = new Matrix4f();
        Matrix4f m2 = new Matrix4f();

        if (w > h) {
            float aspect = ((float) w) / h;
            m1.loadFrustum(-aspect, aspect, -1, 1, 1, 100);
        } else {
            float aspect = ((float) h) / w;
            m1.loadFrustum(-1, 1, -aspect, aspect, 1, 100);
        }

        m2.loadRotate(180, 0, 1, 0);
        m1.loadMultiply(m1, m2);

        m2.loadScale(-1, 1, 1);
        m1.loadMultiply(m1, m2);

        m2.loadTranslate(0, 0, 1);
        m1.loadMultiply(m1, m2);
        return m1;
    }

    private void updateProjectionMatrices() {
        Matrix4f projNorm = getProjectionNormalized(mWidth, mHeight);
        ScriptField_VpConsts.Item i = new ScriptField_VpConsts.Item();
        i.MVP = projNorm;
        i.scaleSize = densityDPI / 240.0f;
        mPvConsts.set(i, 0, true);
    }

    private Allocation loadTexture(int id) {
        final Allocation allocation = Allocation.createFromBitmapResource(mRS, mRes, id);
        return allocation;
    }

    private void loadTextures() {
        dotAllocation = loadTexture(R.drawable.dot);
        beamAllocation = loadTexture(R.drawable.beam);
        bgAllocation = loadTexture(R.drawable.bg);
        mScript.set_textureDot(dotAllocation);
        mScript.set_textureBeam(beamAllocation);
        mScript.set_textureBg(bgAllocation);
    }

    private void createProgramVertex() {
        ProgramVertexFixedFunction.Constants mPvOrthoAlloc =
            new ProgramVertexFixedFunction.Constants(mRS);
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(mWidth, mHeight);
        mPvOrthoAlloc.setProjection(proj);
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        ProgramVertex pv = pvb.create();
        ((ProgramVertexFixedFunction) pv).bindConstants(mPvOrthoAlloc);
        mScript.set_vertBg(pv);

        updateProjectionMatrices();

        ProgramVertex.Builder builder = new ProgramVertex.Builder(mRS);
        builder.setShader(mRes, R.raw.dot_vs);
        builder.addConstant(mPvConsts.getType());
        builder.addInput(dotMesh.getVertexAllocation(0).getType().getElement());
        ProgramVertex pvs = builder.create();
        pvs.bindConstants(mPvConsts.getAllocation(), 0);
        mRS.bindProgramVertex(pvs);
        mScript.set_vertDots(pvs);

    }

    private void createProgramFragment() {
        Sampler.Builder samplerBuilder = new Sampler.Builder(mRS);
        samplerBuilder.setMinification(NEAREST);
        samplerBuilder.setMagnification(NEAREST);
        samplerBuilder.setWrapS(WRAP);
        samplerBuilder.setWrapT(WRAP);
        Sampler sn = samplerBuilder.create();
        ProgramFragmentFixedFunction.Builder builderff =
            new ProgramFragmentFixedFunction.Builder(mRS);
        builderff = new ProgramFragmentFixedFunction.Builder(mRS);
        builderff.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                ProgramFragmentFixedFunction.Builder.Format.RGB, 0);
        ProgramFragment pfff = builderff.create();
        mScript.set_fragBg(pfff);
        pfff.bindSampler(sn, 0);

        ProgramFragment.Builder builder = new ProgramFragment.Builder(mRS);
        builder.setShader(mRes, R.raw.dot_fs);
        builder.addTexture(Program.TextureType.TEXTURE_2D);
        ProgramFragment pf = builder.create();
        pf.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);
        mScript.set_fragDots(pf);

    }

    private void createProgramRaster() {
        ProgramRaster.Builder builder = new ProgramRaster.Builder(mRS);
        builder.setPointSpriteEnabled(true);
        ProgramRaster pr = builder.create();
        mRS.bindProgramRaster(pr);
    }

    private void createProgramFragmentStore() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
        builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE);
        mRS.bindProgramStore(builder.create());
    }

    public void start() {
        mRS.bindRootScript(mScript);
    }

    public void stop() {
        mRS.bindRootScript(null);
    }

    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mScript.set_xOffset(xOffset);
    }

    public void resize(int w, int h) {
        // why do i need to do this again when surface changed for wallpaper, but not when as an app?
        ProgramVertexFixedFunction.Constants mPvOrthoAlloc =
            new ProgramVertexFixedFunction.Constants(mRS);
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(w, h);
        mPvOrthoAlloc.setProjection(proj);

        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        ProgramVertex pv = pvb.create();
        ((ProgramVertexFixedFunction) pv).bindConstants(mPvOrthoAlloc);
        mScript.set_vertBg(pv);
    }

}
