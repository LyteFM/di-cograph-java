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

#include <assert.h>
#include <stdio.h>
#include "test.h"

/**
 * Returns the relation between set set 'a' and 'b'
 */
int testset(const family_t *f, int a,int b)
{
  int amb=0,bma=0,aib=0;
  int i;
  for(i=0;i<f->grnd_size;i++)
    f->grnd_count[i]=0;

  for(i=0;i<f->sets[a].size;i++)
    f->grnd_count[f->sets[a].set[i]]++;

  for(i=0;i<f->sets[b].size;i++) {
    if(f->grnd_count[f->sets[b].set[i]]) aib++;
    else bma++; 
    f->grnd_count[f->sets[b].set[i]]=0;
  }

  for(i=0;i<f->sets[a].size;i++) {
    if(f->grnd_count[f->sets[a].set[i]]) amb++;
    f->grnd_count[f->sets[a].set[i]]=0;
  }
  if(amb && bma && aib) return OVERLAP;
  if(amb==0 && bma==0) return EQUAL;
  if(amb==0) return SUPERSET;
  if(bma==0) return SUBSET;
  if(aib==0) return DISJOIN;
  assert(0);
  return 0;
}


/**
 * Compute the complete overlap graph.
 * For testing purpose only.
 * Time: not linear...
 */
void graph_overlap_create(graph_t *g,const family_t *f)
{
  int i,j;

  graph_create(g,f->size);

  for(i=0;i<f->size;i++)
    for(j=i+1;j<f->size;j++) {
      int r=testset(f,i,j);
      if(r==OVERLAP) 
	graph_add_edge(g,i,j);
    }
  
  graph_sort(g);
}

