# datomic-seed

A Clojure program designed to load seed data for testing from edn files into a 
Datomic database. The edn files are preprocessed to allow for creating 
relationships between entities using keywords rather than numeric tempids.

## Usage

The main method of `net.phobot.datomic.seed` will run load seed data into a
Datomic database whose URL may be specified using a command-line argument. The 
command line also accepts a `-d` or `--data` option indicating the directory 
where the seed data files are to be found. If this option is not specified, the 
default value is a subdirectory of the current working directory, named 
`seed-data`. After the data is loaded, the main method calls `datomic/shutdown` 
to clean up resources. To create a standalone executable jar, use `lein uberjar`.

The command line also will accept a `-s` or `--schema-dir` option indicating 
the directory where schema migration files are to be found. If this option is 
specified, the database will be dropped and recreated using the schema migrations
before the seed data is loaded.  The [datomic-migrator](https://github.com/psfblair/datomic-migrator) library is used 
for running migrations.

This library can also be called from other code using the `seed-database` 
function, which accepts as arguments the URL of the database, the directory 
containing the migrations, the directory containing the seed data, and a function 
to handle logging statements. If the argument specifying the migration directory
is `nil`, the function will only load seed data; otherwise it drops and recreates
the database. When this function is called, the `datomic/shutdown` method is not 
called after the migrations finish.

The project assumes that seed data files and migrations are named using a naming 
schema that ensures proper sort order, and will run them in sort order. It does 
not use Conformity to make idempotent inserts of seed data; rather, it assumes
that you will generally want to recreate the database from scratch each time.
(This also means that you need not worry about modifying the seed data files during 
ongoing development, as opposed to schema migrations where the best practice is
to leave them unchanged once they are committed.)

## Extensions to edn Syntax

This library includes handlers for several new tagged expressions in edn files.
These are `#db/keyid`, `#db/lookupid`, and `#db/lookupids`. These allow you to
establish relationships between entities using keywords. For example:

    { :db/id #db/keyid[:some/partition :category/sports]
      :category/name "Sports"}
    { :db/id #db/keyid[:some/partition :subcategory/golf]
      :subcategory/name "Golf"}
    { :db/id #db/keyid[:some/partition :subcategory/tennis]
      :subcategory/name "Tennis"}
      ...
    { :db/id #db/lookupid :category/sports
      :category/subcategories #db/lookupids[ :subcategory/golf :subcategory/tennis ...]
    }

The edn files are preprocessed; each instance of `#db/keyid` creates an entry in
a symbol table with a new tempid, and the #db/keyid and #db/lookupid elements 
are replaced with corresponding #db/id tagged elements containing the tempids.

## License

```
The MIT License (MIT)

Copyright (c) 2016 Paul Blair

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
