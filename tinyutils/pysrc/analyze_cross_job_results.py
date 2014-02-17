#!/usr/local/bin/python
#-------------------------------------------------------------------------------
# UW SPF - The University of Washington Semantic Parsing Framework
# <p>
# Copyright (C) 2013 Yoav Artzi
# <p>
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or any later version.
# <p>
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
# details.
# <p>
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc., 51
# Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
#-------------------------------------------------------------------------------
import sys, math
from optparse import OptionParser

'''
    Get mean and standard deviation for cross-validation job output files.
'''

def to_float(s):
    try:
        return float(s)
    except:
        return s

def parse_line(line):
    return dict(map(lambda x: (x[0], to_float(x[1])), map(lambda x: x.rsplit('=', 1), line.strip().split('\t'))))

def std(nums):
    # Sample standard deviation
    return 0.0 if len(nums) == 1 else math.sqrt(sum(map(lambda x: math.pow(x - mean(nums), 2), nums)) / (len(nums) - 1))

def mean(nums):
    return sum(nums) / float(len(nums))


if __name__ == '__main__':
    # Parse command line arguments
    parser = OptionParser(usage = 'usage: %prog -m metric -s statistic [-t] output_files')
    parser.add_option('-m', '--metric', dest = 'metric', help = 'Name of metric to average, e.g. EXACT.')
    parser.add_option('-s', '--statistic', dest = 'statistic', help = 'Name of statistic to average, e.g. skippingF1.')
    parser.add_option('-t', '--sort', dest = 'sort', action = 'store_true', help = 'Sort output from smallest mean to largest.')
    parser.add_option('-d', '--dataset', dest = 'dataset', help = 'Dataset ID (should be the prefix of the experiment).')
    parser.add_option('-v', '--verbose', dest = 'verbose', action = 'store_true', default = False, help = 'Verbose output')
    (options, args) = parser.parse_args()

    for metric in options.metric.split(','):
        for statistic in options.statistic.split(','):
            pairs = []
            aggregate = []
            files = []
            for file in args:
                for line in open(file):
                    line_dict = parse_line(line)
                    if line_dict['metric'] == metric and ((not options.dataset) or (line_dict['expId'].endswith(options.dataset))):
                        aggregate.append(line_dict[statistic])
                        files.append(file)
            pairs.append((mean(aggregate), '%s, %s: %.4f +- %.4f  (SE: %.4f)%s' % (metric, statistic, mean(aggregate), std(aggregate), std(aggregate) / math.sqrt(len(aggregate)), (' <= ' + ' '.join(files)) if options.verbose else (' [%d]' % (len(files))))))

            if options.sort:
                pairs.sort(key = itemgetter(0))

            for p in pairs:
                print >> sys.stdout, p[1]


