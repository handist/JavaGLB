# ARGUMENTS for this gnuplot script
#########################################
# ARG1 : number of lines to plot
# ARG2 : time the computation took
# ARG3 : input file
RANGE = ARG1
MAX_CURVE = ARG2
DATA = ARG3

reset
set terminal pngcairo size 1000,700 enh font ",18"

set datafile separator ";"
set style data lines

set logscale y
set noxtics
set tmargin 2
set bmargin 4
set xrange [0:RANGE]
set xtics 60
set xlabel "elapsed time (s)"
set ylabel "grain size"

plot for [i=0:MAX_CURVE] DATA u i*2+1:i*2+2  title sprintf("%s%i","h",i) with steps
