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
import android.renderscript.Mesh.Primitive;
import android.graphics.Color;
import android.renderscript.Float3;
import android.renderscript.Float4;

public class PhaseBeamRS {

    public static final int DOT_COUNT = 28;
    private Resources mRes;
    private RenderScriptGL mRS;
    private ScriptC_phasebeam mScript;
    int mHeight;
    int mWidth;

    private ScriptField_VpConsts mPvConsts;
    private Allocation dotAllocation;
    private Allocation beamAllocation;

    private ScriptField_Particle dotParticles;
    private Mesh dotMesh;

    private ScriptField_Particle beamParticles;
    private Mesh beamMesh;

    private ScriptField_VertexColor_s vertexColors;

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
            createBackgroundMesh();
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

    private void createBackgroundMesh() {
        // The composition and colors of the background mesh were plotted on paper and photoshop
        // first then translated to the code you see below. Points and colors are not random.

        vertexColors = new ScriptField_VertexColor_s(mRS, 48);

        Float3 a = new Float3(-1.25f, 1.0f, 0.0f);
        Float3 b = new Float3(0.0f, 1.0f, 0.0f);
        Float3 c = new Float3(1.25f, 1.0f, 0.0f);
        Float3 d = new Float3(-0.875f, 0.3f, 0.0f);
        Float3 e = new Float3(-0.5f, 0.4f, 0.0f);
        Float3 f = new Float3(0.25f, 0.2f, 0.0f);
        Float3 g = new Float3(0.0f, 0.2f, 0.0f);
        Float3 h = new Float3(-0.625f, 0.1f, 0.0f);
        Float3 i = new Float3(-1.25f, -0.2f, 0.0f);
        Float3 j = new Float3(-0.125f, -0.6f, 0.0f);
        Float3 k = new Float3(-1.25f, -1.0f, 0.0f);
        Float3 l = new Float3(1.25f, -1.0f, 0.0f);
        vertexColors.set_position(0, a, false);
        vertexColors.set_color(0, (new Float4(0.0f,0.584f,1.0f, 1.0f)), false);
        vertexColors.set_position(1, i, false);
        vertexColors.set_color(1, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(2, d, false);
        vertexColors.set_color(2, (new Float4(0.51f,0.549f,0.929f, 1.0f)), false);
        vertexColors.set_position(3, a, false);
        vertexColors.set_color(3, (new Float4(0.0f,0.584f,1.0f, 1.0f)), false);
        vertexColors.set_position(4, d, false);
        vertexColors.set_color(4, (new Float4(0.51f,0.549f,0.929f, 1.0f)), false);
        vertexColors.set_position(5, e, false);
        vertexColors.set_color(5, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(6, a, false);
        vertexColors.set_color(6, (new Float4(0.0f,0.584f,1.0f, 1.0f)), false);
        vertexColors.set_position(7, e, false);
        vertexColors.set_color(7, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(8, b, false);
        vertexColors.set_color(8, (new Float4(0.573f,0.863f,1.0f, 1.0f)), false);
        vertexColors.set_position(9, b, false);
        vertexColors.set_color(9, (new Float4(0.573f,0.863f,1.0f, 1.0f)), false);
        vertexColors.set_position(10, e, false);
        vertexColors.set_color(10, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(11, f, false);
        vertexColors.set_color(11, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(12, b, false);
        vertexColors.set_color(12, (new Float4(0.573f,0.863f,1.0f, 1.0f)), false);
        vertexColors.set_position(13, f, false);
        vertexColors.set_color(13, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(14, c, false);
        vertexColors.set_color(14, (new Float4(0.188f,0.533f,0.882f, 1.0f)), false);
        vertexColors.set_position(15, c, false);
        vertexColors.set_color(15, (new Float4(0.188f,0.533f,0.882f, 1.0f)), false);
        vertexColors.set_position(16, f, false);
        vertexColors.set_color(16, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(17, l, false);
        vertexColors.set_color(17, (new Float4(0.29f,0.31f,0.392f, 1.0f)), false);
        vertexColors.set_position(18, f, false);
        vertexColors.set_color(18, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(19, e, false);
        vertexColors.set_color(19, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(20, g, false);
        vertexColors.set_color(20, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(21, f, false);
        vertexColors.set_color(21, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(22, g, false);
        vertexColors.set_color(22, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(23, l, false);
        vertexColors.set_color(23, (new Float4(0.29f,0.31f,0.392f, 1.0f)), false);
        vertexColors.set_position(24, g, false);
        vertexColors.set_color(24, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(25, e, false);
        vertexColors.set_color(25, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(26, h, false);
        vertexColors.set_color(26, (new Float4(0.251f,0.62f,0.851f, 1.0f)), false);
        vertexColors.set_position(27, h, false);
        vertexColors.set_color(27, (new Float4(0.251f,0.62f,0.851f, 1.0f)), false);
        vertexColors.set_position(28, e, false);
        vertexColors.set_color(28, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(29, d, false);
        vertexColors.set_color(29, (new Float4(0.51f,0.549f,0.929f, 1.0f)), false);
        vertexColors.set_position(30, d, false);
        vertexColors.set_color(30, (new Float4(0.51f,0.549f,0.929f, 1.0f)), false);
        vertexColors.set_position(31, i, false);
        vertexColors.set_color(31, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(32, h, false);
        vertexColors.set_color(32, (new Float4(0.251f,0.62f,0.851f, 1.0f)), false);
        vertexColors.set_position(33, j, false);
        vertexColors.set_color(33, (new Float4(0.157f,0.122f,0.506f, 1.0f)), false);
        vertexColors.set_position(34, h, false);
        vertexColors.set_color(34, (new Float4(0.251f,0.62f,0.851f, 1.0f)), false);
        vertexColors.set_position(35, i, false);
        vertexColors.set_color(35, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(36, j, false);
        vertexColors.set_color(36, (new Float4(0.157f,0.122f,0.506f, 1.0f)), false);
        vertexColors.set_position(37, i, false);
        vertexColors.set_color(37, (new Float4(0.196f,0.745f,1.0f, 1.0f)), false);
        vertexColors.set_position(38, k, false);
        vertexColors.set_color(38, (new Float4(0.357f,0.0f,0.408f, 1.0f)), false);
        vertexColors.set_position(39, l, false);
        vertexColors.set_color(39, (new Float4(0.29f,0.31f,0.392f, 1.0f)), false);
        vertexColors.set_position(40, j, false);
        vertexColors.set_color(40, (new Float4(0.157f,0.122f,0.506f, 1.0f)), false);
        vertexColors.set_position(41, k, false);
        vertexColors.set_color(41, (new Float4(0.357f,0.0f,0.408f, 1.0f)), false);
        vertexColors.set_position(42, g, false);
        vertexColors.set_color(42, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(43, h, false);
        vertexColors.set_color(43, (new Float4(0.251f,0.62f,0.851f, 1.0f)), false);
        vertexColors.set_position(44, j, false);
        vertexColors.set_color(44, (new Float4(0.157f,0.122f,0.506f, 1.0f)), false);
        vertexColors.set_position(45, l, false);
        vertexColors.set_color(45, (new Float4(0.29f,0.31f,0.392f, 1.0f)), false);
        vertexColors.set_position(46, g, false);
        vertexColors.set_color(46, (new Float4(0.467f,0.522f,0.827f, 1.0f)), false);
        vertexColors.set_position(47, j, false);
        vertexColors.set_color(47, (new Float4(0.157f,0.122f,0.506f, 1.0f)), false);

        vertexColors.copyAll();

        Mesh.AllocationBuilder backgroundBuilder = new Mesh.AllocationBuilder(mRS);
        backgroundBuilder.addIndexSetType(Primitive.TRIANGLE);
        backgroundBuilder.addVertexAllocation(vertexColors.getAllocation());
        mScript.set_gBackgroundMesh(backgroundBuilder.create());

        mScript.bind_vertexColors(vertexColors);

    }

    private Allocation loadTexture(int id) {
        final Allocation allocation = Allocation.createFromBitmapResource(mRS, mRes, id);
        return allocation;
    }

    private void loadTextures() {
        dotAllocation = loadTexture(R.drawable.dot);
        beamAllocation = loadTexture(R.drawable.beam);
        mScript.set_textureDot(dotAllocation);
        mScript.set_textureBeam(beamAllocation);
    }

    private void createProgramVertex() {
        ProgramVertex.Builder backgroundBuilder = new ProgramVertex.Builder(mRS);
        backgroundBuilder.setShader(mRes, R.raw.bg_vs);
        backgroundBuilder.addInput(ScriptField_VertexColor_s.createElement(mRS));
        ProgramVertex programVertexBackground = backgroundBuilder.create();
        mScript.set_vertBg(programVertexBackground);

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
        ProgramFragment.Builder backgroundBuilder = new ProgramFragment.Builder(mRS);
        backgroundBuilder.setShader(mRes, R.raw.bg_fs);
        ProgramFragment programFragmentBackground = backgroundBuilder.create();
        mScript.set_fragBg(programFragmentBackground);

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

    }

}
