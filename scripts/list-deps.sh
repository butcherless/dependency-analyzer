#!/bin/bash

dir_list="dependency-analyzer"

for prj in $dir_list
do
  echo "looking for modules in project: $prj"
  modules=$(sbt --error "print printModules")
  for mod in $modules
  do
    echo "[project,module] -> [$prj,$mod]"
    sbtCmd+=$(printf "\"%s %s %s\" " $mod/dependencyList/toFile /tmp/$prj-$mod-dep-list.log "-f")
  done
done

eval "sbt $sbtCmd"
