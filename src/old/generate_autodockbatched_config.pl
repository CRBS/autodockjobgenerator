#!/usr/bin/perl

use Getopt::Long;
use Pod::Usage;


if (@ARGV == 0){
  pod2usage(2);
}


my $cmdLineParseResult = GetOptions ("autodockdir=s"        => \$blastDir,
                                     "runbenchmark"  => \$runBenchmark,
                                     "batchfactor=i" => \$batchFactor,
                                     "autodockconfig=s" => \$blastConfig,
                                     "failedjobs=s"  => \$failedJobs,
                                     "autodockbatcherconfig=s" => \$blastBatcherConfig,
                                     "help|?"     => \$helpArg,
                                     "man"        => \$manArg) or pod2usage(2);

if ($helpArg){
  pod2usage(1);
}

if ($manArg){
  pod2usage(-exitstatus => 0, -verbose => 2);
}

if (! -d $blastDir){
  print STDERR "$blastDir is not a directory\n";
  exit(1);
}

my $FAILED_JOBS_FILE = "failed.jobs";
my $CONFIG_FILE_NAME = "autodockbatcher.sh.config";
my $BLAST_CONFIG_FILE = "autodock.config";

if (!defined($blastBatcherConfig)){
   $blastBatcherConfig = "$blastDir/$CONFIG_FILE_NAME";
}

# simple case write out a config to just run the first
# job 
if (defined($runBenchmark)){
   open(DATA,">$blastBatcherConfig") || die $!;
   print DATA "1:::1-1\n";
   close(DATA);
   exit(0);
}

if (!defined($batchFactor)){
   print STDERR "--batchfactor must be specified\n";
   pod2usage(1);
}

if (!defined($blastConfig)){
   $blastConfig = "$blastDir/$BLAST_CONFIG_FILE";
}

if (!defined($failedJobs)){
   $failedJobs = "$blastDir/$FAILED_JOBS_FILE";
}

# more complicated task
# read contents of batch.factor file
# batch jobs up based on batch factor
# Jobs to batch will either be the list
# of jobs in failed.jobs file or all jobs
# after the 1st.  

my @jobs = getListOfJobs($failedJobs,$blastConfig);
my $jobIndex = 1;
my $startId = undef;
open(DATA,">$blastBatcherConfig") || die $!;

while(@jobs > 0){

   # create sub array
   my @subArray;
   for (my $x = 0; $x < $batchFactor; $x++){
     #break if we run out of jobs
     if (@jobs <= 0){
        last;
     }
     push(@subArray,shift(@jobs));

   }

   print DATA $jobIndex.":::".getTaskList(\@subArray)."\n";
   $jobIndex++;
}
close(DATA);

exit(0);


sub getTaskList {
   my $subArray = shift;
   my $startId = ${$subArray}[0];
   my $curId = $startId;
   my $taskList = "";
   for (my $z = 1; $z < @${subArray}; $z++){
      if ($curId+1 == ${$subArray}[$z]){
         $curId = ${$subArray}[$z];
      }
      else {
         if ($taskList ne ""){
            $taskList .= ",";
         }
         $taskList .= $startId."-".$curId;
         $startId = ${$subArray}[$z];
         $curId = $startId;
      }
   }
   if ($taskList ne ""){
       $taskList .= ",";
   }
   $taskList .= $startId."-".$curId;
   return $taskList;
}


#
# Generates sorted array of jobs to run.  If failed.jobs
# file exists jobs will be from that file otherwise it will
# be every job minus the first job.
#
sub getListOfJobs {
   my $failedJobsFile = shift;
   my $blastConfig = shift;
   my @jobs;

   if (-e $failedJobsFile){
      open(DATA,$failedJobsFile) || die $!;
      while(<DATA>){
         chomp();
         push(@jobs,$_);
      }
      close(DATA);
   }
   else {
      open(DATA,$blastConfig) || die $!;
      my $last = undef;
      while(<DATA>){
	$last = $_;
      }
      close(DATA);
      chomp($last);
      $last=~s/:::.*//;
      for (my $x = 2; $x <= $last; $x++){
         push(@jobs,$x);
      }
   }

   my @sortedJobs = sort { $a <=> $b } @jobs;
   
   return @sortedJobs;
}

1;

__END__

=head1 NAME

generate_autodockbatcher_config - Batches Auto dock jobs

=head1 SYNOPSIS

generate_autodockbatcher_config [ B<options> ]

=head1 DESCRIPTION

B<generate_autobatcher_config> creates a autodockbatcher.sh.config file in the
path specified by the user.  What jobs go into the configuration file depends
on options specified below.  The format of the configuration file will always
be of the following:

(Job Index):::#-#,#-#,#-#

Example:

1:::1-3,4-4

2:::10-24

=head1 OPTIONS

=over 4

=item B<--autodockdir directory>
 
Blast directory where various files used by this program are read from and written to.

=item B<--runbenchmark>

If set only the first job is written to autodockbatcher.sh.config configuration file

=item B<--autodockconfig file>

Sets alternate autodock.config file to read from.  

=item B<--failedjobs file>

Sets alternate failed.jobs file

=item B<--batchfactor integer>

Sets the batching factor.  This value must be set for all modes except
B<--runbenchmark>

=item B<--autodockbatcherconfig file>

Sets alternate autodockbatcher.sh.config file to write

=back

Z<>

=head1 EXIT STATUS

=over

=item 0     Operation was successful.

=item >0    Error.

=back

=head1 AUTHOR

Christopher Churas <churas@ncmir.ucsd.edu>

=head1 REPORTING BUGS

bugs


=head1 COPYRIGHT

Need to fill this out

=cut



