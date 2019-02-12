# A-Priori-Algorithm-on-NYC-open-Dtata-Set


you will implement the A-priori algorithm.
You can use Java or C or C++. You will submit a single file (AP.java or AP.C) which can be run in the following three modes.

1. java AP -g < file.dat > 1col.txt { this file should be: "UID","your data" }
This mode ("generate") will take an input file and generate 1 column (the additional column) of augmented input e.g. "11pm+" as described in class.

2. java AP -v 0.09 0.31 < small.txt ## show the pruned itemsets
This mode ("verbose") will show your implementation of A-priori on a small data set with the same structure as the main file. In this mode you will use the existing
fields in the file to generate 3 and 4 itemsets.

minsup=0.09
minconf=0.31 (to prune if lower than this)

A c=0.200 pruned
B c=0.200 pruned
C c=0.322 not pruned
D c=0.400 not pruned
E c=0.500 not pruned
C->D c=0.200 pruned
C->E c=0.400 not pruned

minsup  (0.0 to 1.0)
minconf (0.0 to 1.0)

3. java AP -m minsup minconf $F 1col.txt  ## the real thing
This mode ("master") should discover and write the 3-itemset rules and 4-itemset rules to standard output.
e.g.
"Heat","11pm+" => "Electric Heater" (sup = 0.03 conf = 0.40)
