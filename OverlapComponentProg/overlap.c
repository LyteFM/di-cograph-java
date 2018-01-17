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
#include <assert.h>
#include "overlap.h"

/*
#define DEBUG
*/

/**
 * Create a empty family
 * Time: O(grnd_size)
 */
void family_create(family_t *f,int grnd_size) 
{
  int i;
  f->grnd_size=grnd_size;
  f->size=0;
  f->sets=NULL;

  /* structure for checking in O(|X|) if X has no multiple elms*/
  f->grnd_count=(int*)malloc(sizeof(int)*f->grnd_size);
  for(i=0;i<f->grnd_size;i++)
    f->grnd_count[i]=0;
}

/** 
 * Destroy a family
 * Time: O(size+grnd_size)
 */
void family_free(family_t *f)
{
  int i;
  for(i=0;i<f->size;i++) 
    free(f->sets[i].set);
  free(f->sets);
  free(f->grnd_count);
}

void print_set(const int *set,int size) 
{
  int i;
  for(i=0;i<size;i++)
    /*printf("%c ",'a'+set[i]);*/
    printf("%d ",set[i]);
  printf("\n");
}

void family_print(const family_t *f)
{
  int i,j;
  for(i=0;i<f->size;i++) {
    for(j=0;j<f->sets[i].size;j++)
      printf("%d ",f->sets[i].set[j]);
    printf("-1\n");
  }
}

/** 
 * Clear data in the family (max, left, right...)
 */
void family_clear(family_t *f)
{
  int i;
  for(i=0;i<f->size;i++) {
    f->sets[i].max=-1;
    f->sets[i].left=-1;
    f->sets[i].right=-1;
    f->sets[i].ampos=-1;
  }
}

/**
 * Add a set of 'size' elements int f
 * Time: O(size_set) 
 */
int family_add_set(family_t *f, int size_set, const int *set)
{
  int i;

#ifdef DEBUG
  printf("add set: ");
  print_set(set,size_set);

#endif

  /* check if the set is correct */
  assert(size_set>0 && size_set<=f->grnd_size);
  for(i=0;i<size_set;i++) {
    assert(set[i]>=0 && set[i]<f->grnd_size);
    assert(f->grnd_count[set[i]]==0);
    f->grnd_count[set[i]]++;
  }
  for(i=0;i<size_set;i++)
    f->grnd_count[set[i]]=0;


  /* add the set to the family */
  if(f->size==0) f->sets=(set_t*)malloc(sizeof(set_t));
  else f->sets=(set_t*)realloc(f->sets,sizeof(set_t)*(f->size+1));
  f->sets[f->size].size=size_set;
  f->sets[f->size].set=(int*)malloc(size_set*sizeof(int));
  for(i=0;i<size_set;i++) f->sets[f->size].set[i]=set[i];
  f->sets[f->size].max=-1;
  f->sets[f->size].left=-1;
  f->sets[f->size].right=-1;
  f->sets[f->size].ampos=-1;
  f->sets[f->size].id=f->size;
  f->size++;

  return f->size-1;
}

/**
 * Check if sets in 'f' are sorted in decreasing order w.r.t. their size.
 * Time: O(size) 
*/
int family_check_sort(const family_t *f)
{
  int i;
  for(i=1;i<f->size;i++)
    if(f->sets[i-1].size<f->sets[i].size) return 0;
  return 1;
}

/**
 *  Temporary structure to sort a family 
 */
typedef struct set_srt_s {
  set_t set;
  struct set_srt_s *next;
} set_srt_t ;

/**
 * Sort sets in f in decreasing order w.r.t. their size 
 * Time: O(f->grnd_set + \sum_i f->set[i].size) 
 */
void family_sort(family_t *f)
{
  set_srt_t **t;
  int i;
  if(family_check_sort(f)) return;
  
  t=(set_srt_t**)malloc((f->grnd_size+1)*sizeof(set_srt_t*));
  for(i=0;i<=f->grnd_size;i++) 
    t[i]=NULL;
  for(i=0;i<f->size;i++) {
    set_srt_t *e=(set_srt_t*)malloc(sizeof(set_srt_t));
    e->next=t[f->sets[i].size];
    e->set=f->sets[i];
    t[f->sets[i].size]=e;
  }
  int k=0;
  for(i=f->grnd_size-1;i>0;i--) {
    set_srt_t *p=t[i],*pt;
    while(p) {
      f->sets[k++]=p->set;
      pt=p->next;
      free(p);
      p=pt;
    }
  }
  free(t);
}


/*
 *Structures and algo for refining 
 */

/**
 * A class in the refine structure
 */
typedef struct {
  int start,end; /*start and end of the class in 't' */
  int mark; /*temporary marker */
} ref_class_t;

/**
 * A element in the refine structure
 */
typedef struct {
  int member; /* the element in the ground set */
  ref_class_t *clas; /* the class in which 'member' occurs */
  int mark; /*temporary marker */
} ref_elm_t;

/**\brief The refine structure
 *
 * The refine structure has 2 tables, and some classes.
 * - on table 't' (as explained in the paper). 
 *  Each element in 't' has a reference to his class.
 * - on table 'ind' indiced by the elements of the ground set. 
 *  ind[i] is the indice of the element 'i' in 't'
 */
typedef struct {
  int size; /* size of the ground set */
  ref_elm_t *t; 
  int *ind;
} ref_t;

/** 
 * Create a new class in the refine structure.
 * Time: O(1)
 */
static ref_class_t *new_ref_class_t(int start_,int end_) 
{
  ref_class_t *c=(ref_class_t*)malloc(sizeof(ref_class_t));
  c->start=start_;
  c->end=end_;
  c->mark=0;
  return c;
}

/**
 * Create the refine strucutre, with one class
 * Time: O(size)
 */
static void ref_init(ref_t *r,int s)
{
  int i;
  ref_class_t *c;
  r->size=s;
  assert(s>0);
  r->t=(ref_elm_t*)malloc(sizeof(ref_elm_t)*s);
  c=new_ref_class_t(0,s-1);
  r->ind=(int*)malloc(sizeof(int)*s);
  for(i=0;i<r->size;i++) {
    r->t[i].member=i;
    r->t[i].clas=c;
    r->t[i].mark=0;
    r->ind[i]=i;
  }
}

/** 
 * Delete the refine structure
 * Time: O(size) 
 */
static void ref_free(ref_t *r) 
{
  ref_class_t *o=NULL;
  int i;
  for(i=0;i<r->size;i++) {
    if(r->t[i].clas!=o) 
      free(r->t[i].clas);
    o=r->t[i].clas;
  }
  free(r->t);
  free(r->ind);
}

static void ref_print(const ref_t *r,int check)
{
  int i,j;
  for(i=0;i<r->size;i++) {
    assert(i==r->t[r->ind[i]].member);
    /*printf("%c:%d  ",'a'+i,r->ind[i]);*/
    printf("%d:%d  ",i,r->ind[i]);
  }
  printf("\n");
  for(i=0;i<r->size;) {
    ref_class_t *c=r->t[i].clas;
    if(i) printf(",");
    printf("{");
    for(j=c->start;j<=c->end;j++) {
      if(j!=c->start)printf(",");
      /*printf("%c",'a'+r->t[j].member);*/
      printf("%d",r->t[j].member);
      if(!check) printf("(%d,%d)",r->t[j].mark,r->t[j].clas->mark);
      if(check) assert(r->t[j].mark==0);
      if(check) assert(r->t[j].clas->mark==0);
    }
    printf("}");
    i=j;
  }
  printf("\n");
}

/** 
 * Exchange two elements in the structure
 * Time: O(1)
 */
static void xcg(ref_t *r, int a,int b)  
{
  if(a==b) return;
  ref_elm_t tmp=r->t[b];

  int tmp2=r->ind[r->t[b].member];
  r->ind[r->t[b].member]=r->ind[r->t[a].member];
  r->ind[r->t[a].member]=tmp2;

  r->t[b]=r->t[a];
  r->t[a]=tmp;
}

/**
 * Refine by the set 'X' of size 'size_X'
 *  If fct is not NULL, executes fct on every hit classes (for the second pass).
 * Time: O(size_X)
 */
static void refine(ref_t *r,const int *X, int size_X, void (*fct)(void *,const ref_class_t *,int, int, int),void *data) 
{
  int i,j;

  /* classes hit by X */
  ref_class_t **clas=(ref_class_t**)malloc(sizeof(ref_class_t*)*size_X); 
  int nbrclass=0;

#ifdef DEBUG
  printf("refine by: ");print_set(X,size_X);
#endif
  
  for(i=0;i<size_X;i++) {
    int pos;
    ref_class_t *c;
    /*ref_print(r,0);printf("%d %d\n",X[i],r->size);*/
    assert(X[i]>=0 && X[i]<r->size);
    pos=r->ind[X[i]];
    c=r->t[pos].clas;
    /*printf("pos=%d\n",pos);*/
    
    assert(r->t[pos].mark==0);
    r->t[pos].mark++;
    
    if(r->t[pos].clas->mark==0) { 
      /* if mark==0 it is the first time than 
	 the class is hit: we add it in class */
      clas[nbrclass]=c;
      nbrclass++;
    }
    
    /* place 'pos' at the end of the class */
    if(pos<c->end-c->mark) {
      xcg(r,pos,c->end-c->mark);
    }
    
    c->mark++;
  }
  
  /* for all class hit */
  for(i=0;i<nbrclass;i++) {
    ref_class_t *c=clas[i];
    
    if(c->mark<1+c->end-c->start) {
      /*printf("(mark=%d) class %d,%d -> ",c->mark,c->start,c->end);*/
      ref_class_t *c2=new_ref_class_t(c->end-c->mark+1,c->end);
      
      if(fct) fct(data,c,c->start,c->end-c->mark,c->end);
      
      for(j=c->end-c->mark+1;j<=c->end;j++) {
	r->t[j].clas=c2;
	r->t[j].mark=0;
      }

      c->end-=c->mark;
      c->mark=0;

      /*printf("%d,%d + %d,%d\n",c->start,c->end,c2->start,c2->end);*/
    } else { /* all the class is a subset of X: unmark */
      for(j=c->start;j<=c->end;j++) {
	r->t[j].mark=0;
      }
      c->mark=0;
    }
  }

#ifdef DEBUG
  ref_print(r,1);
#endif
}

/**
 * Returns 'Left' and 'Right' for the set 'X'.
 * Also return the members of the ground set corresponding to 'left' and 'right'.
 * Time: O(size_X) 
 */
static void leftright(const ref_t *r,const int *X, int size_X,int *left,int *right, int *mleft,int *mright) 
{
  int i;
  assert(size_X>0);
  for(i=0;i<size_X;i++) {
    if(i==0 || *left>r->ind[X[i]]) {
      *left=r->ind[X[i]];
      *mleft=r->t[*left].member;
    }
    if(i==0 || *right<r->ind[X[i]]) {
      *right=r->ind[X[i]];
      *mright=r->t[*right].member;
    }
  }
}


/* AM structure */

/**
 * Element of a AM structure:
 * - identifier of a set
 * - a boolean saying if the set is removed
 */
typedef struct {
  int set;
  int ok;
} am_elm_t;

/**
 * The AM structure:
 * - 't' is the set of elements 
 * - 'ti[i]' is the indice of the current set with right=i
 */
typedef struct {
  am_elm_t *t;
  int *ti;
} am_t;

/**
 * Temporary structure to sort the AM structure
 */
typedef struct am_srt_s {
  int set;
  struct am_srt_s *next;
} am_srt_t;

/**
 * Create a AM_structure
 * Time: O(f->grnd_size + f->size)
 */
static void am_create(am_t *am, const family_t *f) 
{
  int i,j,k;  
  
  /* temporary table for sorting in O(f->grnd_size) */
  am_srt_t **tt=(am_srt_t**)malloc(sizeof(am_srt_t*)*f->grnd_size); 
  int *ti;

  am->t=(am_elm_t*)malloc(sizeof(am_elm_t)*f->size);
  am->ti=(int*)malloc(sizeof(int)*f->grnd_size);

  /* Number of set with right==i */
  ti=(int*)malloc(sizeof(int)*f->grnd_size); 
  
  for(i=0;i<f->grnd_size;i++) {
    tt[i]=NULL;
    am->ti[i]=0;
    ti[i]=0;
  }
  
  for(i=0;i<f->size;i++) { /*Sort by 'left'. O(f->size) */
    am_srt_t *e=(am_srt_t*)malloc(sizeof(am_srt_t));
    e->next=tt[f->sets[i].left];
    e->set=i;
    tt[f->sets[i].left]=e;
    am->ti[f->sets[i].right]++;
  }

  j=0;
  for(i=0;i<f->size;i++) {
    am->t[i].set=-1;
  }

  for(i=0;i<f->grnd_size;i++) { /* Computes am->ti */
    k=j+am->ti[i];
    am->ti[i]=j;
    j=k;
  }
  
  for(i=0;i<f->grnd_size;i++) { /* Sort by right, and put into am->t
				   O(f->size+f->grnd_size) */
    am_srt_t *p=tt[i],*pt;
    while(p) {
      int right=f->sets[p->set].right;
      int pos=am->ti[right]+ti[right];
#ifdef DEBUG
      printf("am1: %d %d\n",f->sets[p->set].left,f->sets[p->set].right);
#endif
      am->t[pos].set=p->set;
      am->t[pos].ok=1;
      f->sets[p->set].ampos=pos;
      ti[right]++;
      pt=p;
      p=p->next;
      free(pt);
    }
  }

#ifdef DEBUG
  for(i=0;i<f->size;i++) { 
    if(am->t[i].set==-1)
      printf("am2 %d: NULL\n",i);
    else 
      printf("am2 %d: %d %d\n",i,f->sets[am->t[i].set].left,f->sets[am->t[i].set].right);
  }

  for(i=0;i<f->grnd_size;i++) {
    printf("am3 %d: %d\n",i,am->ti[i]);
  }
#endif

  free(tt);
  for(i=0;i<f->grnd_size;i++) {
#ifdef DEBUG
    printf("amc %d: %d\n",i,ti[i]);
#endif
    assert(ti[i]==(i+1==f->grnd_size?f->size:am->ti[i+1])-am->ti[i]);
  }
  free(ti);
}

/**
 * Destroy a AM_structure
 * Time: O(f->grnd_size + f->size)
 */
static void am_free(am_t *am)
{
  free(am->t);
  free(am->ti);
} 

/**
 * Temporaty structure for the fct_test fucntion
 */ 
typedef struct {
  am_t *am;
  family_t *f;
  int set;
} fct_data_t;

/**
 * function which is executed on the second pass to compute Maxs
 * Overall runing time in O(f->grnd_size + f->size)
 */
static void fct_test(void *data,const ref_class_t *c, int start, int end, int end2)
{
  fct_data_t *d=(fct_data_t*)data;
  am_t *am=d->am;
  family_t *f=d->f;
  int i;
  
  for(i=end+1;i<=end2;i++) {
    while(1) {
      if(am->ti[i]==f->size) 
	break; /*end of the AM structure */
      
      int set=am->t[am->ti[i]].set; 
      if(f->sets[set].right!=i) 
	break; /*there is no more sets with right=i*/
      if(am->t[am->ti[i]].ok==0) 
	/* if ok==0, the set is already removed form the structure */
	am->ti[i]++;
      else if(f->sets[set].left<=end) {
	/*otherwise, this is the first time that left(X) and right(X) are 
	  separated by a set Y. Thus Max(X)=Y */
	f->sets[set].max=d->set;
	am->ti[i]++;
      } else break;
    }
  }
}

/**
 * Compute Maxs.
 * Time: O(f->grnd_size + \sum_i f->sets[i].size)
 */
void compute_max(family_t *f)
{
  int i;
  ref_t *r;
  am_t *am;
  int op;
  
  family_sort(f);

  /* 1st refining */ 

  r=(ref_t*)malloc(sizeof(ref_t));
  ref_init(r,f->grnd_size);
  for(i=0;i<f->size;i++)
    refine(r,f->sets[i].set,f->sets[i].size,NULL,NULL);


  /* comute left and right for all sets */

  for(i=0;i<f->size;i++) {
    leftright(r,f->sets[i].set,f->sets[i].size,
	      &(f->sets[i].left),&(f->sets[i].right),
	      &(f->sets[i].mleft),&(f->sets[i].mright)
	      );
#ifdef DEBUG
    printf("%d: left=%d right=%d\n",i,(f->sets[i].left),(f->sets[i].right));
#endif
  }
  ref_free(r);
  
  am=(am_t*)malloc(sizeof(am_t));
  am_create(am,f);
  
  /* 2nd refining */ 

  ref_init(r,f->grnd_size);  
  op=0;
  for(i=0;i<f->size;i++) {
    fct_data_t *data=(fct_data_t*)malloc(sizeof(fct_data_t)); 
    data->am=am;
    data->set=i;
    data->f=f;
    refine(r,f->sets[i].set,f->sets[i].size,fct_test,data);
    free(data);

    if(i==f->size-1 || f->sets[i+1].size!=f->sets[i].size) {
      /* there is no more X' with |X'|=|X|:
         remove from AM all X' with |X'|=|X| */    
      for(;op<=i;op++)
	am->t[f->sets[op].ampos].ok=0;
    }
  }
  
  ref_free(r);
  free(r);
  am_free(am);
  free(am);

#ifdef DEBUG
  for(i=0;i<f->size;i++) {
    printf("set: ");print_set(f->sets[i].set,f->sets[i].size);
    printf(" max=%d\n",f->sets[i].max);
    if(f->sets[i].max>=0) {
      printf(" max: ");print_set(f->sets[f->sets[i].max].set,f->sets[f->sets[i].max].size);
    }
  }
#endif
}

/* SL structure */

/**
 * Element of a SL structure 
 */
typedef struct sl_elm_s {
  int set;
  struct sl_elm_s *next;
} sl_elm_t;

/**
 * SL structure (table of lists)
 */
typedef struct {
  int size;
  sl_elm_t **t;
} sl_t;

/**
 * Create a SL structure
 * Each list is sorted in <_LF order
 * Time: O(f->grnd_size + \sum_i f->sets[i].size)
 */
static void sl_create(sl_t *s,const family_t *f)
{
  int i,j;
  s->t=(sl_elm_t**)malloc(sizeof(sl_elm_t*)*f->grnd_size);
  s->size=f->grnd_size;
  for(i=0;i<f->grnd_size;i++) {
    s->t[i]=NULL;
  }
  for(i=0;i<f->size;i++) {
    for(j=0;j<f->sets[i].size;j++) {
      int k=f->sets[i].set[j];
      sl_elm_t *e=(sl_elm_t*)malloc(sizeof(sl_elm_t));
      e->next=s->t[k];
      e->set=i;
      s->t[k]=e;
    }
  }
}

/**
 * Destroy SL structure
 * Time: O(f->grnd_size + \sum_i f->sets[i].size)
 */
static void sl_free(sl_t *s)
{

  int j;
  for(j=0;j<s->size;j++) {
    sl_elm_t *p=s->t[j],*pt;
    while(p) {
      pt=p->next;
      free(p);
      p=pt;
    }
  }
  free(s->t);
}


/**
 * Create a empty graph of 'n' vertices
 * Time: O(n)
 */
void graph_create(graph_t *g, int n)
{
  int i;
  g->n=n;
  g->t=(edge_t**)malloc(sizeof(edge_t*)*g->n);
  for(i=0;i<g->n;i++)
    g->t[i]=NULL;
}

void graph_free(graph_t *g)
{
  int i;
  for(i=0;i<g->n;i++) {
    edge_t *e=g->t[i],*et;
    while(e) {
      et=e->next;
      free(e);
      e=et;
    }
  }
  free(g->t);
}

static void graph_add_half_edge(graph_t *g,int i,int j)
{
  edge_t *e=(edge_t*)malloc(sizeof(edge_t));
  e->v=i;
  e->next=g->t[j];
  g->t[j]=e;
}

/**
 * Add an edge in g
 * Time: constant
 */
void graph_add_edge(graph_t *g,int i,int j)
{
  assert(i!=j && i>=0 && j>=0 && i<g->n && j<g->n);
  graph_add_half_edge(g,i,j);
  graph_add_half_edge(g,j,i);
}

/** 
 * Sort adjacency lists of 'g' and remove multiple edges.
 * Time: linear in the size of g
 */
void graph_sort(graph_t *g)
{
  graph_t g2;
  int i;

  graph_create(&g2,g->n);
  for(i=g->n-1;i>=0;i--) {
    edge_t *e=g->t[i];
    while(e) {
      if(g2.t[e->v]==NULL || g2.t[e->v]->v!=i)
	graph_add_half_edge(&g2,i,e->v);
      e=e->next;
    }
  }

  graph_free(g);
  *g=g2;
}

void graph_print(const graph_t *g)
{
  int i;
  for(i=0;i<g->n;i++) {
    edge_t *e=g->t[i];
    printf("%d:",i);
    while(e) {
      printf(" %d",e->v);
      e=e->next;
    }
    printf("\n");
  }
}

/** 
 * Computes the Dahlhaus graph 
 * Time: O(f->grnd_size + \sum_i f->set[i].size)
 */
void graph_dahlhaus_create(graph_t *g,const family_t *f)
{
  int i;
  sl_t sl;

  graph_create(g,f->size);
  sl_create(&sl,f);

  for(i=0;i<sl.size;i++) {
    sl_elm_t *e=sl.t[i];
    int smax=-1;
    while(e) {
      int set=e->set;
      if(f->sets[set].max>=0 && f->sets[f->sets[set].max].size>smax)
	smax=f->sets[f->sets[set].max].size;
      if(e->next!=NULL) {
	int set2=e->next->set;
	if(f->sets[set2].size<=smax)
	  graph_add_edge(g,set,set2);
      }
      e=e->next;
    }
  }

  graph_sort(g);

  sl_free(&sl);
}

typedef struct quintuple_s {
  int left,right;
  int x,y,maxx;
  struct quintuple_s *next;
} quintuple_t;

/**
 * Reverse the order of a list of quintuples
 * Time: O(size)
 */
static void reverse_quintuple_list(quintuple_t **q)
{
  quintuple_t *q2=NULL,*t;
  while(*q) {
    t=(*q)->next;
    (*q)->next=q2;
    q2=*q;
    *q=t;
  }
  *q=q2;
}

/**
 * Computes a subgraph of the overlap graph
 * Time: O(f->grnd_size + \sum_i f->set[i].size )
 */
void graph_subgraph_overlap_create(graph_t *g,const family_t *f)
{
  int i;
  sl_t sl;

  quintuple_t **ql=NULL,**qr=NULL;
  
  /* quintuples sorted by left and right */
  ql=(quintuple_t **)malloc(f->grnd_size*sizeof(quintuple_t *));
  qr=(quintuple_t **)malloc(f->grnd_size*sizeof(quintuple_t *));

  for(i=0;i<f->grnd_size;i++)
    ql[i]=qr[i]=NULL;

  graph_create(g,f->size);
  sl_create(&sl,f);

  for(i=0;i<sl.size;i++) {
    sl_elm_t *e=sl.t[i];
    int x,maxx;
    int smax=-1;
    while(e) {
      int set=e->set;
      if(f->sets[set].max>=0) 
	graph_add_edge(g,set,f->sets[set].max);
      
      if(smax>=0 && f->sets[set].size<=smax && set!=maxx) {
	/* create the quintuple and put it into 'ql */
	quintuple_t *p=(quintuple_t*)malloc(sizeof(quintuple_t));
	p->left=f->sets[set].mleft;
	p->right=f->sets[set].mright;
	p->x=x;
	p->y=set;
	p->maxx=maxx;
	p->next=ql[p->left];
	ql[p->left]=p;
      }

      if(f->sets[set].max>=0 && f->sets[f->sets[set].max].size>smax) {
	x=set;
	maxx=f->sets[set].max;
	smax=f->sets[f->sets[set].max].size;
      }
      
      e=e->next;
    }
  }

  /* reverse the lists of quintiples: after that every list is sorted in <_LF*/
  for(i=0;i<f->grnd_size;i++)
    reverse_quintuple_list(&(ql[i]));
  
  /* for every list, compare with SL(i) */
  for(i=0;i<f->grnd_size;i++) {
    quintuple_t *p=ql[i];
    sl_elm_t *p2=sl.t[i];
    
    while(p) {
      quintuple_t *t;
      while(p2 && p2->set < p->y) p2=p2->next;
      t=p->next;
      if(p2 && p2->set==p->y) {
	/* if the element is in the list (BM(r,left(X))=1), put the quintiple
	   in qr */
	p->next=qr[p->right];
	qr[p->right]=p;
	p2=p2->next;
      } else {
	/* otherwise Y is adjacent to X */
	graph_add_edge(g,p->y,p->x);
	free(p);
      }
      
      p=t;
    }
  }

  /* reverse the lists of quintiples: after that every list is sorted in <_LF*/
  for(i=0;i<f->grnd_size;i++)
    reverse_quintuple_list(&(qr[i]));

  for(i=0;i<f->grnd_size;i++) {
    quintuple_t *p=qr[i];
    sl_elm_t *p2=sl.t[i];
    
    while(p) {
      quintuple_t *t;
      while(p2 && p2->set <p->y) p2=p2->next;
      t=p->next;
      if(p2 && p2->set==p->y) {
	/* Y is adjacent to Max(X) */
	graph_add_edge(g,p->y,p->maxx);
	p2=p2->next;
      } else {
	/* Y is adjacent to X */
	graph_add_edge(g,p->y,p->x);
      }
      free(p);
      
      p=t;
    }
  }

  graph_sort(g);

  sl_free(&sl);

  free(ql);
  free(qr);
}

/**
 * Compute a DFS in 'g' stating at vertex 'i'
 * 't' is the table of already visited vertices
 * 'p' is the number of the connected component
 */
static void dfs(const graph_t *g,int *t,int i,int p)
{
  edge_t *e=g->t[i];
  if(t[i]) return;
  t[i]=p;
  while(e) {
    dfs(g,t,e->v,p);
    e=e->next;
  }
}

/**
 * Computes the connected components of 'g', and put them into 't'.
 * (each connected component has a different number).
 * Returns the number of connected components
 */ 
int graph_connected_components(const graph_t *g,int *t)
{
  int i,p=0;
  
  for(i=0;i<g->n;i++) t[i]=0;
  
  for(i=0;i<g->n;i++)
    if(t[i]==0) {
      p++;
      dfs(g,t,i,p);
    }

  return p;
}
