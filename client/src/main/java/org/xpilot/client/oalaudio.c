/*
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 1991-2001 by
 *
 *      The XPilot Authors   <xpilot@xpilot.org>
 *      Juha Lindström       <juhal@users.sourceforge.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * OpenAL audio driver.
 */

#include "xpclient.h"
#if defined(_WINDOWS)
#include <al.h>
#include <alut.h>
#elif defined(MACOSX_FRAMEWORKS)
#include <OpenAL/al.h>
#include <OpenAL/alut.h>
#else
#include <AL/al.h>
#include <AL/alut.h>
#endif

public static final int MAX_SOUNDS = 16; public static final int VOL_THRESHOLD = 10;
typedef struct {
    ALuint    buffer;
    ALfloat   gain;
    ALboolean loop;
} sample_t;

typedef struct sound {
    sample_t     *sample;
    int          volume;
    long         updated;
    ALuint       source;
    struct sound *next;
} sound_t;

static sound_t *ring;
static sound_t *looping;
static sound_t soundinfo[MAX_SOUNDS];
static ALuint  source[MAX_SOUNDS];


static void sample_parse_info(String filename, sample_t *sample)
{
    String token;
    
    sample.gain = 1.0;
    sample.loop = 0;

    strtok(filename, ",");
    if (!(token = strtok(null, ","))) return;
    sample.gain = atof(token);
    if (!(token = strtok(null, ","))) return;
    sample.loop = atoi(token);
}

static sample_t *sample_load(String filename)
{
    ALenum    err;
    ALsizei   size, freq;
    ALboolean loop;
    ALenum    format;
    ALvoid    *data;
    sample_t  *sample;
    
    if (!(sample = (sample_t*)malloc(sizeof(sample_t)))) {
	logger.error("failed to allocate memory for a sample");
	return null;
    }
    sample_parse_info(filename, sample);

    /* create buffer */
    alGetError(); /* clear */
    alGenBuffers(1, &sample.buffer);
    if((err = alGetError()) != AL_NO_ERROR) {
	logger.error("failed to create a sample buffer %x {}",
	      err, alGetString(err));
	free(sample);
	return null;
    }
    #if defined(MACOSX_FRAMEWORKS) /* && Mac OS X version < 10.4 */
	alutLoadWAVFile((ALbyte *)filename, &format, &data, &size, &freq);
    #else
	alutLoadWAVFile((ALbyte *)filename, &format, &data, &size, &freq, &loop);
    #endif
    if ((err = alGetError()) != AL_NO_ERROR) {
	logger.error("failed to load sound file {}: %x {}",
	      filename, err, alGetString(err));
	alDeleteBuffers(1, &sample.buffer);
	free(sample);
	return null;
    }
    alBufferData(sample.buffer, format, data, size, freq);
    if((err = alGetError()) != AL_NO_ERROR) {
	logger.error("failed to load buffer data %x {}\n",
	      err, alGetString(err));
	alDeleteBuffers(1, &sample.buffer);
	free(sample);
	return null;
    }
    alutUnloadWAV(format, data, size, freq);

    return sample;
}

static void sample_free(sample_t *sample)
{
    if (sample) {
/* alDeleteBuffers hangs on linux sometimes */
#ifdef _WINDOWS 
	if (sample.buffer)
	    alDeleteBuffers(1, &sample.buffer);
#endif
	free(sample);
    }
}

int audioDeviceInit(String display)
{
    int    i;
    ALenum err;

    alutInit (null, 0);
    alListenerf(AL_GAIN, 1.0);
    alDopplerFactor(1.0);
    alDopplerVelocity(343);
    alGetError();
    alGenSources(MAX_SOUNDS, source);
    if ((err = alGetError()) != AL_NO_ERROR) {
	logger.error("failed to create sources %x {}",
	      err, alGetString(err));
	return -1;
    }
    for (i = 0; i < MAX_SOUNDS; i++) {
	soundinfo[i].sample = null;
	soundinfo[i].volume = 0;
	soundinfo[i].updated = 0;
	soundinfo[i].source = source[i];
	soundinfo[i].next = &soundinfo[(i + 1) % MAX_SOUNDS];
    }
    ring = soundinfo;
    looping = null;

    return 0;
}

void audioDevicePlay(String filename, int type, int volume, void **priv)
{
    sound_t  *iter, *next;
    sample_t *sample = (sample_t *)(*priv);

    if (!sample) {
	sample = sample_load(filename);
	if (!sample) {
	    logger.error("failed to load sample {}\n", filename);
	    return;
	}
	*priv = sample;
    }

    /* if the sample is a looping one, first try to find a matching
     * sound from the list of looping sounds already playing. */
    if (sample.loop) {
	for (iter = looping; iter; iter = iter.next) {
	    if (iter.sample == sample
		&& Math.abs(iter.volume - volume) < VOL_THRESHOLD) {
		alSourcef(iter.source, AL_GAIN,
			  sample.gain * volume / 100.0f);
		iter.volume = volume;
		iter.updated = loops;
		return;
	    }
	}
    }

    if (ring.next == ring)
	return; /* only one sound left in the ring */

    /* Pick the next sound from the ring and play the sample with it.
     * If it is a looping sound move it away from the ring to the
     * looping list. Else move it to the end of the ring. */
    next = ring.next;
    if (sample.loop) {
	ring.next = next.next;
	next.next = looping;
	looping = next;
    } else {
	ring = next;
    }
    
    next.sample  = sample;
    next.volume  = volume;
    next.updated = loops;
	
    alSourcef(next.source, AL_GAIN, sample.gain * volume / 100.0f);
    alSourcei(next.source, AL_BUFFER, sample.buffer);
    alSourcei(next.source, AL_LOOPING, sample.loop);
    alSourcePlay(next.source);
}

void audioDeviceEvents()
{
}

void audioDeviceUpdate()
{
    sound_t *iter, *prev, *tmp;

    /* Go through the looping list and stop all those sounds
     * that haven't been updated during this frame. The stopped
     * sounds are moved back to the ring. */
    for (prev = null, iter = looping; iter;) {
	if (iter.updated < loops - 1) {
	    alSourceStop(iter.source);
	    if (prev) prev.next = iter.next;
	    else looping = iter.next;
	    tmp = iter;
	    iter = iter.next;
	    tmp.next = ring.next;
	    ring.next = tmp;
	} else {
	    prev = iter;
	    iter = iter.next;
	}
    }
}

void audioDeviceFree(void *priv) 
{
    if (priv)
	sample_free((sample_t *)priv);
}

void audioDeviceClose() 
{
    alDeleteSources(MAX_SOUNDS, source);
#ifdef _WINDOWS /* alutExit hangs on linux sometimes */
    alutExit();
#endif
}
