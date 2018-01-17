CCOPT=-g -O3 -Wall -ansi

all: main

main: main.o overlap.o test.o gen.o
	gcc $(CCOPT) -o main main.o overlap.o test.o gen.o

main.o: main.c overlap.h
	gcc -c $(CCOPT) main.c

overlap.o: overlap.c overlap.h
	gcc -c $(CCOPT) overlap.c

gen.o: gen.c gen.h overlap.h
	gcc -c $(CCOPT) gen.c

test.o: test.c test.h overlap.h
	gcc -c $(CCOPT) test.c

clean:
	rm main *.o *~
