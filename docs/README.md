### Documentation
The documentation at [replaymod.com/docs](https://www.replaymod.com/docs/) is generated from
`content.md` using a custom Node.js script.

Using an image directly after a header will show it to the right of the text and will show the following
text as its caption.

Block quotes (`> Quote`) are used to highlight sections of texts: `> **Note:** This is important.`

Images should be available in two formats: jpg, which is loaded by default and the original png which
is loaded when the user clicks on the image. Note that some might only be available as jpg right now
as their original was lost.  
Animated images should be available in webm (vp9) format which is displayed by default and as a gif which is used as
the fallback for browsers that lack webm support.  
In `content.md`, the default version is used.

Make sure you have [npm]() installed.
For the initial setup run `npm install`.
Run `node build` to generate the html.

Note: The template used for the live version is different and the one here is only for convenient testing.