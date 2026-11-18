# Content

Shared content for the Traveler app: the texts, coordinates and sources of the
attractions in Sinaia. Everything here is written and checked by the team before
it reaches the apps, so a visitor never reads a fact nobody verified.

Owner of this folder: see [`.github/CODEOWNERS`](../.github/CODEOWNERS).

## Layout

| Path | What it holds |
| --- | --- |
| [`attractions/README.md`](attractions/README.md) | The list of 10 attractions (proposed until the team agrees on it) |
| `attractions/<NN>-<slug>.md` | One file per researched attraction, for example `01-peles-castle.md` |

`NN` is the position in the agreed list (two digits) and `slug` is the English
name in lower case with hyphens.

## File format of an attraction

Each attraction is one Markdown file. It starts with YAML front matter:

```yaml
---
id: peles-castle              # same as the slug, stable
order: 1                      # position in the agreed list
name:
  ro: Castelul Peleș
  en: Peleș Castle
coordinates:
  lat: 45.36009               # decimal degrees, 5 decimals
  lon: 25.54273
image:
  file: pending               # filled in when the photo is added
  licence: pending
  author: pending
  source: pending
status: researched
---
```

`status` is one of `proposed` (on the list, not researched yet), `researched`
(texts, coordinates and sources are complete) or `approved` (reviewed by the
team). The `image` fields stay `pending` until the photo task is done; do not
guess a licence.

The body has these sections, in this order:

1. `## Description (RO)` and `## Description (EN)`: 60 to 120 words each, in the
   team's own words (no text copied from a source). Romanian uses the correct
   comma-below letters ș and ț and the letters ă â î.
2. `## Facts`: a table `fact | source 1 | source 2`. Every row is one fact and
   carries two full `https` URLs.
3. `## Coordinates`: the two sources, the distance between their values, and
   which value was chosen and why.
4. `## Unverified (not used)`: facts that have only one source, or where the
   sources disagree. They stay out of the descriptions and the app.

## The two-source rule

Every fact (date, name, number, who built it, what it is) needs **two
independent sources**. A fact with one source is not used; it goes under
`## Unverified (not used)` so the next researcher can find a second source.

What counts as independent:

- Different publishers that did not copy each other. Good pairs: the official
  website of the site or its museum plus a heritage register, an encyclopedia,
  a national news agency or a travel guide.
- All language editions of Wikipedia count as one source, and Wikidata counts
  with Wikipedia. Wikivoyage counts separately from Wikipedia.
- A page that repeats the wording of another (for example a site that reuses
  the museum's history text) does not count as a second source.
- When two sources disagree, the fact is not used, even if one of them is the
  official site. Write the disagreement under `## Unverified (not used)`.

Every URL in a table was opened and read by the researcher on the date given in
the file. Opening hours, ticket prices and closing days change every season and
differ between sites, so they are not stored here.

## Coordinates

Coordinates are used for the QR check-in within 50 m, so they must be accurate.

- Decimal degrees with 5 decimals (about 1 m).
- Two sources per point. The researcher states the distance between them.
- If the two values are more than 50 m apart, the value that matches the
  visitor entrance or the main building is chosen, and the file explains why.
- The 50 m check-in radius is small for large sites (a castle, a monastery, a
  park). The place of the QR code has to be confirmed on site before release.

## Images

The owner of the folder adds the photos herself. A photo needs a known licence,
its author and its source page before the `image` fields are filled in.
