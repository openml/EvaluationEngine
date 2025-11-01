# Releasing Openml Croissant
## Create release branch
Create a release branch from `develop`, named `release/v[MAJOR].[MINOR].[PATCH].

## Update version and changelog
Update the version in [pom.xml](pom.xml).

## Merge
Merge back to `develop`, don't delete the branch.
Merge back to `master`.

## Create a tag
Create a tag, named `v[MAJOR].[MINOR].[PATCH]`. A docker container will automatically be build
and published.

## Deploy
Deploy the new version to be used. [TODO].