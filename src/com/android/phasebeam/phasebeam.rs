#pragma version(1)

#pragma rs java_package_name(com.android.phasebeam)

#include "rs_graphics.rsh"
#pragma stateVertex(parent);
#pragma stateStore(parent);

rs_allocation textureDot;
rs_allocation textureBeam;
rs_allocation textureBg;

rs_program_vertex vertBg;
rs_program_fragment fragBg;

rs_program_vertex vertDots;
rs_program_fragment fragDots;

int numBeamParticles;
int numDotParticles;

typedef struct __attribute__((packed, aligned(4))) Particle {
    float3 position;
    float offsetX;
} Particle_t;

typedef struct VpConsts {
    rs_matrix4x4 MVP;
    float scaleSize;
} VpConsts_t;
VpConsts_t *vpConstants;

Particle_t *dotParticles;
Particle_t *beamParticles;
rs_mesh dotMesh;
rs_mesh beamMesh;

float densityDPI;
float xOffset = 0.5;

float screenWidth;
float screenHeight;
float halfScreenWidth;
float quarterScreenWidth;

float newOffset = 0.5;

void positionParticles(){
    screenWidth = rsgGetWidth();
    screenHeight = rsgGetHeight();
    halfScreenWidth = screenWidth/2.0f;
    quarterScreenWidth = screenWidth/4.0f;

    Particle_t* particle = dotParticles;
    numDotParticles = rsAllocationGetDimX(rsGetAllocation(dotParticles));
    for(int i=0; i<numDotParticles; i++){
        particle->position.x = rsRand(0.0f, 3.0f);
        particle->position.y = rsRand(-1.25f, 1.25f);

        float z;
        if(i < 2){
            z = 14.0f;
        } if(i < 3){
            z = 25.0f;
        } else if(i < 4){
            z = rsRand(10.f, 20.f);
        } else if(i == 5){
            z = 24.0f;
            particle->position.x = 1.0;
        } else {
            z = rsRand(4.0f, 10.0f);
        }
        particle->position.z = z;
        particle->offsetX = 0;

        particle++;
    }

    Particle_t* beam = beamParticles;
    numBeamParticles = rsAllocationGetDimX(rsGetAllocation(beamParticles));
    for(int i=0; i<numBeamParticles; i++){
        float z;
        if(i < 10){
            z = rsRand(4.0f, 10.0f)/2.0f;
        } else {
            z = rsRand(4.0f, 35.0f)/2.0f;
        }
        beamParticles->position.x = rsRand(-1.25f, 1.25f);
        beamParticles->position.y = rsRand(-1.05f, 1.205f);

        beamParticles->position.z = z;
        beamParticles->offsetX = 0;
        beamParticles++;
    }
}

int root(){

    rsgClearColor(0.0f, 0.f, 0.f,1.0f);

    rsgBindProgramVertex(vertBg);
    rsgBindProgramFragment(fragBg);
    rsgBindTexture(fragBg, 0, textureBg);
    rsgDrawRect(-quarterScreenWidth + xOffset*quarterScreenWidth, 0.0f,
        screenWidth+halfScreenWidth + xOffset*quarterScreenWidth, screenHeight, 0.0f);

    Particle_t* beam = beamParticles;
    Particle_t* particle = dotParticles;
    newOffset = xOffset*2;
    for(int i=0; i<numDotParticles; i++){
        if(beam->position.y > 1.05){
            beam->position.y = -1.05;
        } else {
            beam->position.y = beam->position.y + 0.000160*beam->position.z;
        }

        beam->offsetX = newOffset;
        beam++;

        if(particle->position.y > 1.25){
            particle->position.y = -1.25;
        } else {
            particle->position.y = particle->position.y + 0.00022*particle->position.z;
        }

        particle->offsetX = newOffset;
        particle++;
    }

    rsgBindProgramVertex(vertDots);
    rsgBindProgramFragment(fragDots);

    rsgBindTexture(fragDots, 0, textureBeam);
    rsgDrawMesh(beamMesh);

    rsgBindTexture(fragDots, 0, textureDot);
    rsgDrawMesh(dotMesh);

    return 45;
}
