svgEpub
========================

svgEpub は画像ファイルから EPUB ファイルを作成するオーサリングツールです。
画像ファイルはラスター形式あるいはベクター形式に変換したもの(SVG)を選択できます。

The svgEpub is an authoring tool which creates an EPUB file from image files. 
User can choose image files itself or SVG files which converted from image files.

Download
========

Version 1.0
* Binary Archive
 * [svgEpub-version-1.0-0.zip](https://github.com/downloads/nirvash/svgEpub/svgEpub-1.0-0.zip)
* Source archive
 * [nirvash-svgEpub-version-1.0-0.zip](https://github.com/nirvash/svgEpub/zipball/version-1.0)

Install
========

1. Install Java Runtime. (http://java.com/en/download/index.jsp)
1. Unzip downloaded archive.
1. Move to nirvash-svgEpub-7fd76ce\svgEpub\binary
1. Execute svgEpub.jar.

  
### optional for Windows : use opencv for more better conversion
* opencv を有効にすると変換精度があがります。
* Install the runtime components of Microsoft Visual C++ 2010
 * [Microsoft Visual C++ 2010 Redistributable Package (x86)](http://www.microsoft.com/download/en/details.aspx?id=5555)
 * [Microsoft Visual C++ 2010 Redistributable Package (x64)](http://www.microsoft.com/download/en/details.aspx?id=14632)
* Extract [OpenCV-2.4.2.exe](http://sourceforge.net/projects/opencvlibrary/files/opencv-win/2.4.2/OpenCV-2.4.2.exe/download) inside the root directory C:\
* Set enable_opencv to yes in config dialog.

Limitation
========
* I verified the application only in windows environment.


Contact
=====================

Please report problems or requests to [Issues](https://github.com/nirvash/svgEpub/issues).

Using the Library
=================

* [potrace](http://potrace.sourceforge.net/)
* [Batik SVG Toolkit](http://xmlgraphics.apache.org/batik/)
* [Epublib](http://www.siegmann.nl/epublib)
* [Apache Commons Compress™](http://commons.apache.org/compress/)
* [Apache Commons Logging](http://commons.apache.org/logging/)

License
=======
This application is provided unser the the open source (revised) BSD license.
