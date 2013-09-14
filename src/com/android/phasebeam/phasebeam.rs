#pragma version(1)

#pragma rs java_package_name(com.android.phasebeam)

#include "rs_graphics.rsh"
#pragma stateVertex(parent);
#pragma stateStore(parent);

rs_allocation textureDot;
rs_allocation textureBeam;

rs_program_vertex vertBg;
rs_program_fragment fragBg;

rs_program_vertex vertDots;
rs_program_fragment fragDots;

static int numBeamParticles;
static int numDotParticles;
static int numVertColors;

typedef struct __attribute__((packed, aligned(4))) Particle {
    float3 position;
    float offsetX;
    float3 adjust;
} Particle_t;

typedef struct VpConsts {
    rs_matrix4x4 MVP;
    float scaleSize;
} VpConsts_t;
VpConsts_t *vpConstants;

typedef struct VertexColor_s {
    float3 position;
    float offsetX;
    float4 color;
    float4 realColor;
    float3 adjust;
} VertexColor;

VertexColor* vertexColors;
Particle_t *dotParticles;
Particle_t *beamParticles;
rs_mesh dotMesh;
rs_mesh beamMesh;
rs_mesh gBackgroundMesh;

float3 adjust = { -1.0, 1.0, 1.0 };
float3 oldAdjust = { -1.0, 1.0, 1.0 };
float densityDPI;
float xOffset = 0.5;

static float screenWidth;
static float screenHeight;
static float halfScreenWidth;
static float quarterScreenWidth;
static float quarterScreenHeight;
static float halfScreenHeight;

static float newOffset = 0.5;
static float oldOffset = 0.5;

static const float zxParticleSpeed = 0.0000780;
static const float zxBeamSpeed = 0.00005;
static const float yzParticleSpeed = 0.00011;
static const float yzBeamSpeed = 0.000080;

void positionParticles() {
    screenWidth = rsgGetWidth();
    screenHeight = rsgGetHeight();
    halfScreenWidth = screenWidth/2.0f;
    halfScreenHeight = screenHeight/2.0f;
    quarterScreenWidth = screenWidth/4.0f;
    quarterScreenHeight = screenHeight/4.0f;
    rs_allocation aParticles = rsGetAllocation(dotParticles);
    numDotParticles = rsAllocationGetDimX(rsGetAllocation(dotParticles));
    numVertColors = rsAllocationGetDimX(rsGetAllocation(vertexColors));
    for(int i=0; i<numDotParticles; i++) {
        Particle_t* particle = (Particle_t *) rsGetElementAt(aParticles, i);
        particle->position.x = rsRand(0.0f, 3.0f);
        particle->position.y = rsRand(-1.25f, 1.25f);

        float z;
        if (i < 3) {
            z = 14.0f;
        } else if(i < 7) {
            z = 25.0f;
        } else if(i < 4) {
            z = rsRand(10.f, 20.f);
        } else if(i == 10) {
            z = 24.0f;
            particle->position.x = 1.0;
        } else {
            z = rsRand(6.0f, 14.0f);
        }
        particle->position.z = z;
        particle->offsetX = 0;
        particle->adjust = adjust;
    }

    Particle_t* beam = beamParticles;
    numBeamParticles = rsAllocationGetDimX(rsGetAllocation(beamParticles));
    for(int i=0; i<numBeamParticles; i++) {
        float z;
        if(i < 20) {
            z = rsRand(4.0f, 10.0f)/2.0f;
        } else {
            z = rsRand(4.0f, 35.0f)/2.0f;
        }
        beamParticles->position.x = rsRand(-1.25f, 1.25f);
        beamParticles->position.y = rsRand(-1.05f, 1.205f);

        beamParticles->position.z = z;
        beamParticles->offsetX = 0;
        beamParticles->adjust = adjust;
        beamParticles++;
    }
}

int root() {
    float speedbump;

    newOffset = xOffset*2;
    speedbump = newOffset != oldOffset ? 0.25 : 1.0;
    rsgClearColor(0.0f, 0.f, 0.f,1.0f);

    if(newOffset != oldOffset
            || oldAdjust.x != adjust.x
            || oldAdjust.y != adjust.y
            || oldAdjust.z != adjust.z) {
        VertexColor* vert = vertexColors;
        bool useAdjust = adjust.x >= 0;
        for(int i=0; i<numVertColors; i++) {
            vert->offsetX = -xOffset/2.0;
            vert->realColor = vert->color;
            if (useAdjust) {
                float grey = 0.3 * vert->color.x + 0.59 * vert->color.y + 0.11 * vert->color.z;
                vert->realColor.x = grey;
                vert->realColor.y = grey;
                vert->realColor.z = grey;
            }
            vert->adjust = adjust;
            vert++;
        }
    }

    rsgBindProgramVertex(vertBg);
    rsgBindProgramFragment(fragBg);

    rsgDrawMesh(gBackgroundMesh);

    Particle_t* beam = beamParticles;
    Particle_t* particle = dotParticles;

    for (int i=0; i<numBeamParticles; i++) {
        if(beam->position.x/beam->position.z > 0.5) {
            beam->position.x = -1.0;
        }
        if(beam->position.y > 1.15) {
            beam->position.y = -1.15;
            beam->position.x = rsRand(-1.25f, 1.25f);
        } else {
            beam->position.y += yzBeamSpeed * beam->position.z * speedbump;
        }
        beam->position.x += zxBeamSpeed * beam->position.z * speedbump;
        beam->offsetX = newOffset;
        beam->adjust = adjust;
        beam++;
    }

    for(int i=0; i<numDotParticles; i++) {
        if(particle->position.x/particle->position.z > 0.5) {
            particle->position.x = -1.0;
        }

        if(particle->position.y > 1.25) {
            particle->position.y = -1.25;
            particle->position.x = rsRand(0.0f, 3.0f);

        } else {
            particle->position.y += yzParticleSpeed * particle->position.z * speedbump;
        }

        particle->offsetX = newOffset;
        particle->position.x += zxParticleSpeed * beam->position.z * speedbump;
        particle->adjust = adjust;
        particle++;
    }

    rsgBindProgramVertex(vertDots);
    rsgBindProgramFragment(fragDots);

    rsgBindTexture(fragDots, 0, textureBeam);
    rsgDrawMesh(beamMesh);

    rsgBindTexture(fragDots, 0, textureDot);
    rsgDrawMesh(dotMesh);

    oldOffset = newOffset;
    oldAdjust = adjust;

    return 66 * speedbump;
}
