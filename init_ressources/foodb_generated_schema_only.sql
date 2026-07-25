PRAGMA foreign_keys = OFF;
BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS compound (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  public_id TEXT NULL,
  name TEXT NULL,
  state TEXT NULL,
  annotation_quality TEXT NULL,
  description TEXT NULL,
  cas_number TEXT NULL,
  moldb_smiles TEXT NULL,
  moldb_inchi TEXT NULL,
  moldb_mono_mass TEXT NULL,
  moldb_inchikey TEXT NULL,
  moldb_iupac TEXT NULL,
  kingdom TEXT NULL,
  superklass TEXT NULL,
  klass TEXT NULL,
  subklass TEXT NULL
);

CREATE TABLE IF NOT EXISTS content (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  source_id INTEGER NULL,
  source_type TEXT NULL,
  food_id INTEGER NULL,
  orig_food_id TEXT NULL,
  orig_food_common_name TEXT NULL,
  orig_food_scientific_name TEXT NULL,
  orig_food_part TEXT NULL,
  orig_source_id TEXT NULL,
  orig_source_name TEXT NULL,
  orig_content TEXT NULL,
  orig_min TEXT NULL,
  orig_max TEXT NULL,
  orig_unit TEXT NULL,
  orig_citation TEXT NULL,
  citation TEXT NULL,
  citation_type TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  orig_method TEXT NULL,
  orig_unit_expression TEXT NULL,
  standard_content TEXT NULL,
  preparation_type TEXT NULL,
  export INTEGER NULL
);

CREATE TABLE IF NOT EXISTS accessionnumber (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  number TEXT NULL,
  compound_id INTEGER NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  source_id TEXT NULL,
  source_type TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundalternateparent (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  name TEXT NULL,
  compound_id INTEGER NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundexternaldescriptor (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  external_id TEXT NULL,
  annotations TEXT NULL,
  compound_id INTEGER NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundontologyterm (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  compound_id INTEGER NULL,
  export BOOLEAN NULL,
  ontology_term_id INTEGER NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundsubstituent (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  name TEXT NULL,
  compound_id INTEGER NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundsynonym (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  synonym TEXT NULL,
  synonym_source TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  source_id INTEGER NULL,
  source_type TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundsenzyme (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  compound_id INTEGER NULL,
  enzyme_id INTEGER NULL,
  citations TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundsflavor (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  compound_id INTEGER NULL,
  flavor_id INTEGER NULL,
  citations TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  creator_id TEXT NULL,
  updater_id INTEGER NULL,
  source_id INTEGER NULL,
  source_type TEXT NULL
);

CREATE TABLE IF NOT EXISTS enzyme (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  name TEXT NULL,
  gene_name TEXT NULL,
  description TEXT NULL,
  go_classification TEXT NULL,
  general_function TEXT NULL,
  specific_function TEXT NULL,
  pathway TEXT NULL,
  reaction TEXT NULL,
  cellular_location TEXT NULL,
  signals TEXT NULL,
  transmembrane_regions TEXT NULL,
  molecular_weight TEXT NULL,
  theoretical_pi TEXT NULL,
  locus TEXT NULL,
  chromosome TEXT NULL,
  uniprot_name TEXT NULL,
  uniprot_id TEXT NULL,
  pdb_id TEXT NULL,
  genbank_protein_id TEXT NULL,
  genbank_gene_id TEXT NULL,
  genecard_id TEXT NULL,
  genatlas_id TEXT NULL,
  hgnc_id TEXT NULL,
  hprd_id TEXT NULL,
  organism TEXT NULL,
  general_citations TEXT NULL,
  comments TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS food (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  name TEXT NULL,
  name_scientific TEXT NULL,
  description TEXT NULL,
  itis_id TEXT NULL,
  wikipedia_id TEXT NULL,
  picture_file_name TEXT NULL,
  picture_content_type TEXT NULL,
  picture_file_size INTEGER NULL,
  picture_updated_at TEXT NULL,
  legacy_id INTEGER NULL,
  food_group TEXT NULL,
  food_subgroup TEXT NULL,
  food_type TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  creator_id TEXT NULL,
  updater_id INTEGER NULL,
  export_to_afcdb BOOLEAN NULL,
  category TEXT NULL,
  ncbi_taxonomy_id INTEGER NULL,
  export_to_foodb BOOLEAN NULL,
  public_id TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundspathway (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  compound_id INTEGER NULL,
  pathway_id INTEGER NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS flavor (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  name TEXT NULL,
  flavor_group TEXT NULL,
  category TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL
);

CREATE TABLE IF NOT EXISTS foodtaxonomy (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  food_id INTEGER NULL,
  ncbi_taxonomy_id INTEGER NULL,
  classification_name TEXT NULL,
  classification_order INTEGER NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS compoundshealtheffect (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  compound_id INTEGER NULL,
  health_effect_id INTEGER NULL,
  orig_health_effect_name TEXT NULL,
  orig_compound_name TEXT NULL,
  orig_citation TEXT NULL,
  citation TEXT NULL,
  citation_type TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  source_id INTEGER NULL,
  source_type TEXT NULL
);

CREATE TABLE IF NOT EXISTS healtheffect (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  name TEXT NULL,
  description TEXT NULL,
  chebi_name TEXT NULL,
  chebi_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  chebi_definition TEXT NULL
);

CREATE TABLE IF NOT EXISTS ncbitaxonomymap (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  TaxonomyName TEXT NULL,
  Rank TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS nutrient (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  legacy_id INTEGER NULL,
  public_id TEXT NULL,
  name TEXT NULL,
  export BOOLEAN NULL,
  state TEXT NULL,
  annotation_quality TEXT NULL,
  description TEXT NULL,
  wikipedia_id TEXT NULL,
  comments TEXT NULL,
  dfc_id TEXT NULL,
  duke_id TEXT NULL,
  eafus_id TEXT NULL,
  dfc_name TEXT NULL,
  compound_source TEXT NULL,
  metabolism TEXT NULL,
  synthesis_citations TEXT NULL,
  general_citations TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS ontologysynonym (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  ontology_term_id INTEGER NULL,
  synonym TEXT NULL,
  external_id TEXT NULL,
  external_srouce TEXT NULL,
  parent_id TEXT NULL,
  parent_source TEXT NULL,
  comment TEXT NULL,
  curator TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS pathway (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  smpdb_id TEXT NULL,
  kegg_map_id TEXT NULL,
  name TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL
);

CREATE TABLE IF NOT EXISTS ontologyterm (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  term TEXT NULL,
  definition TEXT NULL,
  external_id TEXT NULL,
  external_source TEXT NULL,
  comment TEXT NULL,
  curator TEXT NULL,
  parent_id INTEGER NULL,
  level INTEGER NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  legacy_id TEXT NULL
);

CREATE TABLE IF NOT EXISTS reference (
  id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
  ref_type TEXT NULL,
  text TEXT NULL,
  pubmed_id TEXT NULL,
  link TEXT NULL,
  title TEXT NULL,
  creator_id TEXT NULL,
  updater_id TEXT NULL,
  created_at TEXT NULL,
  updated_at TEXT NULL,
  source_id INTEGER NULL,
  source_type TEXT NULL
);

COMMIT;
PRAGMA foreign_keys = ON;
