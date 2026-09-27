/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

// --------------------------------------------------------------
// Chargement de la fonction request

#ifdef __APPLE__
#include "__init_unix.h"
#elif __linux__
#include "__init_unix.h"
#elif _WIN32
#include "__asprintf.h"
#include "__init_win.h"
#else
#error "OS not supported"
#endif
