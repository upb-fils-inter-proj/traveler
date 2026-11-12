# Contributing

## Branches

- `main` only changes through pull requests. Nobody pushes to it directly.
- Name a branch after the enabler or user story it belongs to:
  `EN-NN-short-description` or `US-NN-short-description`, for example
  `EN-01-repository-structure`.

## Pull requests

- Fill in the pull request template.
- Close the task or story with `Closes #number`. For an issue in another
  repository use `Closes upb-fils-inter-proj/<repo>#number`.
- A pull request needs one review and a green build before it is merged.
- Request review from the people in [`.github/CODEOWNERS`](.github/CODEOWNERS)
  for the folders you changed. GitHub does this automatically.

## Protection settings for `main`

A repository admin enables these under Settings > Branches > Branch protection
rules, for the branch `main`:

- Require a pull request before merging, with 1 required approval.
- Require status checks to pass before merging, and select the build workflow
  once it exists.
