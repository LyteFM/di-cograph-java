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

#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include "gen.h"

/**
 * Generates a family of sets. 
 * It computes in the same time a inclusion tree, and generate sets which 
 * don't overlap any set in the inclusion tree.
 * We do so to avoid the case where the overlap graph has only one 
 * connected component.
 */


/**
 * Generate a table 't' of 's' random bits.
 */
static int randtab(int *t,int s)
{
  int r=0;
  int i;
  for(i=0;i<s;i++)
    r+=(t[i]=rand()%2);
  return r;
}

/** 
 * Computes the usion of sets in f2.
 * The result is set in 'ta', and the size in 'sa'.
 * 't' is a table of bits saying which set in 'f2' has to be in the union.
 */
static void unionsets(const family_t *f2,int *t, int **ta,int *sa)
{
  int i,p=0,j;
  *sa=0;
  for(i=0;i<f2->size;i++)
    if(t[i]) *sa+=f2->sets[i].size;
  *ta=(int*)malloc(*sa*sizeof(int));
  for(i=0;i<f2->size;i++)
    if(t[i]) {
      for(j=0;j<f2->sets[i].size;j++)
	(*ta)[p++]=f2->sets[i].set[j];
    }
}

/** 
 * Generate 'nbr' sets, usings sets of 'f2' as atoms.
 * Adds the sets in 'f'.
 */
static void gen1(family_t *f, const family_t *f2, int nbr)
{
  int *t;
  /*printf("gen1 %d %d\n",f2->size,nbr);*/
  if(f2->size<3) return;
  t=(int*)malloc(f2->size*sizeof(int));
  while(nbr) {
    int r=randtab(t,f2->size);
    if(r>1 && r<f2->size) {
      int *ta;
      int sa;
      unionsets(f2,t,&ta,&sa);
      family_add_set(f,sa,ta);
      free(ta);
      nbr--;
    }
  }
}


/**
 * Generate a table 't' of 's' random integers from 1 to 'a'
 */
static void randtab2(int *t,int s,int a)
{
  int s2=0;
  int i,o=-1;
  for(i=0;i<s;i++) {
    s2++;
    t[i]=1+(rand()%a);
    if(o==-1) o=t[i];
    if(o>0 && o!=t[i]) o=-2;
  }
  if(s2>1 && o!=-2) /* verify that this is a proper partition */
    randtab2(t,s,a); /* otherwise, retry */
}

/**
 * Generate a proper partition of 'ta', with at most 'degree' classes
 * Add the classes in the family 'f'
 * Runs gen1 on the partition.
 * Recursively runs gen on the classes of the partition
 */
static void gen(family_t *f, int grnd, int *ta, int sa,int degree, float dens)
{
  family_t f2;
  int *tb=(int*)malloc(sa*sizeof(int));
  int *tc=(int*)malloc(sa*sizeof(int));
  int i,r=0,j;
  int d=0;

  if(sa<=1) return;
  
  family_create(&f2,grnd);
  randtab2(tb,sa,degree);
  for(i=1;i<=degree;i++) {
    r=0;
    for(j=0;j<sa;j++) {
      if(tb[j]==i) {
	tc[r++]=ta[j];
      }
    }
    if(r) {
      d++;
      family_add_set(&f2,r,tc);
      /*family_add_set(f,r,tc);*/
      gen(f,grnd,tc,r,degree,dens);
    }
  }
  
  gen1(f,&f2,(d*d)*dens);
  
  family_free(&f2);
  free(tb);
  free(tc);
}

/**
 * Generate a family.
 */ 
void family_gen(family_t *f,int grnd, int degree, float dens,int seed)
{
  static int r=0;
  int *ta=(int*)malloc(grnd*sizeof(int));
  int i;

  if(seed==0)
    srand(time(NULL)+r++);
  else
    srand(seed);
  
  for(i=0;i<grnd;i++)
    ta[i]=i;
  gen(f,grnd,ta,grnd,degree,dens);
  free(ta);
}
