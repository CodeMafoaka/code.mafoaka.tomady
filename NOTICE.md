# Third-party data notices

This repository's own code is licensed under Apache License 2.0 (see `LICENSE`).
That covers the **code** only — the **data** this app fetches, stores, and serves
carries its own, separate license terms from its original source, which this
code's license does not override.

## FooDB

Food composition data synced via `FooDBRemoteSyncService` (`POST /api/v1/foodb/sync`)
originates from [FooDB](https://foodb.ca), maintained by the Wishart Research Group
(University of Alberta / The Metabolomics Innovation Centre).

**License: Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).**

- **Attribution is required** wherever this data is displayed or redistributed:
  "This work is licensed under a Creative Commons Attribution-NonCommercial 4.0
  International License." — see the in-app "Sources de données" credit
  (`ui/screens/ProfileScreen.kt`).
- **Commercial use is restricted.** Per FooDB's own terms: "Use and re-distribution
  of the data, in whole or in part, for commercial purposes requires explicit
  permission of the authors and explicit acknowledgment of the source material
  (FooDB)." **If Tomady is ever monetized or distributed as a commercial product,
  this needs explicit written permission from FooDB before shipping — this is
  not yet obtained as of this writing.**
- Users downloading/redistributing significant portions are asked to cite the
  FooDB paper in any resulting publications.

## USDA FoodData Central

Macro-nutrient (calorie/protein/carb/fat/fiber) data comes from
[USDA FoodData Central](https://fdc.nal.usda.gov) via `NutritionLookupService` /
`UsdaFdcNutrientProvider` (`GET /api/v1/nutrition/macros`).

**License: CC0 1.0 / public domain** (U.S. government work — not copyrighted).
No permission or attribution is legally required, including for commercial use.
Attribution is appreciated but optional; USDA's suggested citation:

> U.S. Department of Agriculture, Agricultural Research Service. FoodData Central, 2019. https://fdc.nal.usda.gov
