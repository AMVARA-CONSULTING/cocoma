

scp /cygdrive/c/Daten/CoCoMa/dist/*.jar rroeber@root@sedcimif0230.emea.isn.corpintra.net@susshi.edc.corpintra.net:/opt/IBM/cognos/workdir/cocoma_v29/.
tar cfvz dist/cocoma.tgz dist/* --exclude=*tgz
cp -rp /cygdrive/c/Daten/CoCoMa/dist/* /cygdrive/i/MIF_U-Mgmt/01_Deployment/00_Deployments/08_Cognos/COCOMA/COCOMA_C1021_V29/.


