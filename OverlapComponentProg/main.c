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

#include <stdio.h>
#include <stdlib.h>
#include "overlap.h"
#include "test.h"
#include "gen.h"

int printgraph=0;
int printCC=1;
int check=0;

int main(int argc, char **argv)
{
  family_t f;
  FILE *in;
  int max=-1;
  int *set;
  int sm=-1;
  int ts=0;
  int *cc1=NULL,*cc2=NULL;
  int nc1,nc2;
  int i,S=0;

  if(argc<=1 || argc >3) {
    printf("usage: '%s file' or '%s size_grnd seed'\n",argv[0],argv[0]);
    exit(1);
  }

  if(argc==3) {
    int grnd=atoi(argv[1]);
    printf("++ Generate the family ++\n");
    family_create(&f,grnd);
    family_gen(&f,grnd,30,0.05,atoi(argv[2]));
  } else {
    /* read the family in a file */
    printf("++ Read the family ++\n");

    if((in=fopen(argv[1],"r"))==NULL) {
      perror("cannot open file\n");
      exit(1);
    }

    /* compute size of the ground set
       we suppose that every element < max is used
       compute also the size max of a set */
    ts=0;
    while(!feof(in)) {
      char buff[1000];
      fscanf(in,"%s",buff);
      if(atoi(buff)>=0) {
          if(atoi(buff)>max) max=atoi(buff);
          ts++;
      } else {
          if(ts>sm) sm=ts;
          ts=0;
      }
    }

    set=(int*)malloc(sizeof(int)*sm);

    fclose(in);

    family_create(&f,max+1);

    if((in=fopen(argv[1],"r"))==NULL) {
      perror("cannot open file\n");
      exit(1);
    }

    ts=0;
    while(!feof(in)) {
      char buff[1000];
      fscanf(in,"%s",buff);
      if(atoi(buff)>=0) {
	set[ts]=atoi(buff);
	ts++;
      } else {
	if(ts>0) {
	  family_add_set(&f,ts,set);
	}
	ts=0;
      }
    }

    free(set);
    fclose(in);
  }

  S=0;
  for(i=0;i<f.size;i++)
    S+=f.sets[i].size;
  printf("++ Ground set: %d\n"
	 "++ Number of sets in the family: %d\n"
	 "++ \\sum_i |X_i| = %d\n",f.grnd_size,f.size,S);

  compute_max(&f);

  {
    graph_t g;
    printf("++ Dahlhaus graph ++\n");
    graph_dahlhaus_create(&g,&f);

    if(printgraph) {
      printf("Graph:\n");
      graph_print(&g);
    }

    cc1=(int*)malloc(sizeof(int)*g.n);
    nc1=graph_connected_components(&g,cc1);

    if(printCC) {
      printf("Connected components:\n");
      for(i=0;i<g.n;i++)
	printf("%d ",cc1[i]);
      printf("\n");
    }
    graph_free(&g);
  }

  family_clear(&f);

  compute_max(&f);

  {
    graph_t g;
    printf("++ A subgraph of the overlap graph ++\n");
    graph_subgraph_overlap_create(&g,&f);

    if(printgraph) {
      printf("Graph:\n");
      graph_print(&g);
    }

    cc2=(int*)malloc(sizeof(int)*g.n);
    nc2=graph_connected_components(&g,cc2);

    if(printCC) {
      printf("Connected components:\n");
      for(i=0;i<g.n;i++)
	printf("%d ",cc2[i]);
      printf("\n");
    }
    graph_free(&g);
  }

  printf("++ %d connected components ++\n",nc1);

  for(i=0;i<f.size;i++) {
    if(cc2[i]!=cc1[i]) {
      printf("++ Something bad happens...\n");
      /*family_print(&f);*/
      exit(1);
    }
  }

  if(check) {
    /* computes the overlap graph by a simple, naive, non polynomial algorithm
     * only for debug */
    graph_t g;
    int *cco;

    printf("++ Overlap graph ++\n");
    graph_overlap_create(&g,&f);

    if(printgraph) {
      printf("Graph:\n");
      graph_print(&g);
    }

    cco=(int*)malloc(sizeof(int)*g.n);
    graph_connected_components(&g,cco);

    if(printCC) {
      printf("Connected components:\n");
      for(i=0;i<g.n;i++)
	printf("%d ",cco[i]);
      printf("\n");
    }

    graph_free(&g);

    for(i=0;i<f.size;i++) {
      if(cco[i]!=cc1[i]) {
	printf("++ Something bad happens... ++\n");
	family_print(&f);
	exit(1);
      }
    }

    free(cco);
  }

  family_free(&f);

  free(cc1);
  free(cc2);

  printf("++ OK ++\n");

  return 0;
}
