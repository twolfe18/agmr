
This is a simple interface for doing MapReduce jobs
on [Annotated Gigaword](http://www.cs.jhu.edu/~vandurme/papers/NapolesGormleyVanDurmeNAACL12.pdf)
using the [agiga library](https://code.google.com/p/agiga/).

To use, just implement either `AGMRDocumentMapper` or `AGMRSentenceMapper` (e.g. see
`TestSentenceMapper`), compile, and run `AGMRRunner`.


