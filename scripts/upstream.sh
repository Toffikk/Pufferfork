# requires curl
# requires awk

exit_on_error() {
    echo "$1"
    exit 1
}

cd ..

oldHash=$(grep "paperRef=" gradle.properties | cut -d "=" -f2)
branch=$(links -dump https://github.com/PaperMC/Paper/branch_commits/$oldHash | awk -F " " '{print $NF}')
cutBranch=$(echo $branch | cut -d' ' -f1)
newHash=$(curl -s https://api.github.com/repos/PaperMC/Paper/commits/$cutBranch | jq -r .sha)

if [ "$oldHash" = "$newHash" ]; then
    echo "Upstream has not updated!"
    exit 0
fi

echo "Updating paper: $oldHash -> $newHash"

sed -i "s/$oldHash/$newHash/g" gradle.properties

./gradlew applyMinecraftSourcePatchesFuzzy -x pufferfish-server:applyPaperMinecraftFeaturePatches || exit_on_error "An error occurred when merging patches!" # revert 
./gradlew rebuildMinecraftSourcePatches -x pufferfish-server:applyPaperMinecraftFeaturePatches || exit_on_error "An error occurred when rebuilding server patches!" # revert!
./gradlew rebuildPaperApiPatches || exit_on_error "An error occurred when rebuilding api patches!"
#./gradlew compileJava || exit_on_error "An error occurred when building!"

git add .

scripts/upstreamCommit.sh $oldHash $newHash

echo "Created new commit, please review before pushing."
