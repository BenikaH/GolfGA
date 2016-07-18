# GolfGA
Genetic algorithm written in Java to find the optimal lineup for daily fantasy sports.

Based off of the DraftKings format of 6 golfers per lineup. An "individual" in the population corresponds to one lineup. Golfers are encoded in binary, with the default being 8 bits per golfer (this is the smallest number of bits that can represent all golfers in the largest fields, around 150 golfers). If golfers are encoded in "x" bits, each "x" bits of a lineup corresponds to one golfer, so the total number of bits to represent one lineup is 6x (48 by default). 

A HashMap "golferNumber" is used to map each possible "x" bit encoding to an integer that represents exactly one golfer. The integer assiged to each golfer is the index that golfer is located at in the ArrayList "golfers", which is assigned as the golfers are read in from a file.

There is one input file to the program, "projections.csv". The program looks for this file in the same folder as the class files. The input file needs to be a comma separated list of golfers and their project point totals. The first row will be ignored because it is assumed to be headers. Similar files can be obtained by going to this link, https://rotogrinders.com/lineup-builder/pga?site=draftkings and downloading the weekly projections. The input file "projections.csv" is contained in the jar file, GolfGA.jar.

The genetic algorithm uses "roulette-wheel" selection. The fitness function is the projected point total for each golfer. The genetic operators used are crossover (one-point crossover at midpoint of encoding) and mutation.

Golfer.java represents each golfer. GolfGA.java contains logic for the genetic algorithm. "projections.csv" is an example input file. Both classes and the input file are contained in the jar file, GolfGA.jar
