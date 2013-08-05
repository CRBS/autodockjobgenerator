#!/usr/bin/perl


if (@ARGV != 3){
  print STDERR "$0 <job dir> <filelist of receptor .pdbqt files> <filelist of ligand .pdbqt files>\n";
  exit(1);
}

my $basedir = $ARGV[0];

open(RECEPTOR,$ARGV[1]) || die $!;

my @receptors = <RECEPTOR>;
close(RECEPTOR);

open(LIGAND,$ARGV[2]) || die $!;

my @ligands = <LIGAND>;
close(LIGAND);

my $configCntr = 1;

open(CONFIG,">$basedir/config/$configCntr.autodock.sh.config") || die $!;


my $cntr = 1;
for (my $x = 0; $x < @receptors; $x++){
  chomp($receptors[$x]);
  $receptor_file_name = $receptors[$x];
  $receptor_file_name=~s/^.*\///;
  $receptor_file_name=~s/\.pdbqt$//;
  mkdir("$basedir/output_pdbqt/$receptor_file_name");
  for (my $y = 0; $y < @ligands; $y++){
    chomp($ligands[$y]);
    $ligand_file = $ligands[$y];
    $ligand_file=~s/^.*\///;
    print CONFIG $cntr.":::--ligand ".$ligands[$y]." --receptor $receptors[$x]\n";
    $cntr++;
    if ($cntr % 200 == 0){
      close(CONFIG);
      $configCntr++;
      open(CONFIG,">$basedir/config/$configCntr.autodock.sh.config") || die $!;
    }
  }
}
exit;
