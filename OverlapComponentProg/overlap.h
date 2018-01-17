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

#ifndef _OVERLAP_H_
#define _OVERLAP_H_

typedef struct {
  int size;
  int *set;

  int left,right;
  int mleft,mright;
  int max; /* -1 : undef */
  int ampos; 
  int id;
} set_t;

typedef struct {
  int size;
  int grnd_size;
  set_t *sets;
  
  int *grnd_count; /* always equal to 0 */
} family_t;

extern void family_create(family_t *f,int grnd_size);
extern void family_free(family_t *f);
extern void family_clear(family_t *f);
extern int family_add_set(family_t *f,int size, const int *set);
extern int family_check_sort(const family_t *f);
extern void family_sort(family_t *f);
extern void family_print(const family_t *f);

extern void compute_max(family_t *f);


typedef struct edge_s {
  int v;
  struct edge_s *next;
} edge_t;

typedef struct  {
  int n;
  edge_t **t;
} graph_t;

extern void graph_free(graph_t *g);
extern void graph_create(graph_t *g,int);
extern void graph_dahlhaus_create(graph_t *g,const family_t *f);
extern void graph_print(const graph_t *g);
extern void graph_sort(graph_t *g);
extern void graph_add_edge(graph_t *g,int i,int j);

extern void graph_subgraph_overlap_create(graph_t *g,const family_t *f);
extern int graph_connected_components(const graph_t *g,int *t);

#endif

