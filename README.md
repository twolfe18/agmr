
This is a simple interface for doing MapReduce jobs (counting only right now)
on [Annotated Gigaword](http://www.cs.jhu.edu/~vandurme/papers/NapolesGormleyVanDurmeNAACL12.pdf)
using the [agiga library](https://code.google.com/p/agiga/).

To use, just implement either `AGMRDocumentMapper` or `AGMRSentenceMapper` (e.g. see
`TestSentenceMapper`), compile, and run `AGMRRunner`.

Note that this code streams over documents and has very low memory usage,
you can easily get away with only using 200MB. Probably half of that if you
really want to, and tweak `AGMRRunner.minMem`.

Beware that the Java sort order is not the same as you might expect from, say, GNU sort.

