/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

#include <pthread.h>

#define DEFINE_TASK(name) \
	static void* name(void* arg)

#define START_TASK(taskid, name, arg) \
	pthread_t taskid; \
	pthread_create(&taskid, NULL, name, arg)

#define JOIN_TASK(taskid) \
	pthread_join(taskid, NULL)

#define DEFINE_MUTEX(name) \
	pthread_mutex_t _ ## name = PTHREAD_MUTEX_INITIALIZER

#define ACQUIRE_MUTEX(name) \
	pthread_mutex_lock(&_ ## name)

#define RELEASE_MUTEX(name) \
	pthread_mutex_unlock(&_ ## name)
