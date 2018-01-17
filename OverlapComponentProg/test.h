/*
 *   This source file is part of program computing set overlap classes 
 *   in linear time.
 *   Copyright (C) 2007  Michael Rao
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef _TEST_H_
#define _TEST_H_

#include "overlap.h"

#define DISJOIN  0
#define SUBSET   1
#define SUPERSET 2
#define EQUAL    3
#define OVERLAP  4

extern int testset(const family_t *f, int a,int b);
extern void graph_overlap_create(graph_t *g,const family_t *f);

#endif
