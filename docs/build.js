require('array-findindex-polyfill')
require('string.prototype.endswith')

var fs = require('fs')
var markdown = require('md-jml')

var input = fs.readFileSync('content.md', 'utf8')
markdown.parse(input, {}, function (tree) {
    // Assign numbers to headers
    var counter = [0, 0, 0, 0]
    var anchorStack = ['']
    for (var i = 2; i < tree.length; i++) {
        var e = tree[i]
        if (typeof e === 'string' || !e[0].match(/h\d/)) {
            continue // Not a header
        }
        var match = / \[(.+)\]/.exec(e[2])
        if (!match) {
            continue // Doesn't contain anchor tag
        }
        var tag = match[1]
        e[2] = /(.+) \[.+\]/.exec(e[2])[1]

        var level = +e[0].substring(1)
        counter[level - 1]++
        for (var j = level; j < 4; j++) {
            counter[j] = 0
        }
        e[1].index = counter.slice(0, level).join('.')
        if (e[1].index.indexOf('.') === -1) {
            // There always has to be at least one dot in the index
            e[1].index += '.'
        }
        // Prepend index to text
        e[2] = e[1].index + ' ' + e[2]

        anchorStack = anchorStack.slice(0, level)
        anchorStack.push(anchorStack[level - 1] + '-' + tag)
        e[1].anchor = anchorStack[level].substring(1)
    }

    function forEachElement (elements, matcher, callback) {
        var indices = elements.map(function (h, index) {
            // Find the indices of all matched children
            return matcher(h) ? index : null
        }).filter(function (i) {
            // Filter out elements that didn't match
            return i !== null
        })
        for (var i = 0; i < indices.length; i++) {
            var element = elements[indices[i]]
            var subElements = elements.slice(indices[i] + 1, i + 1 < indices.length ? indices[i + 1] : elements.length)
            callback(element, subElements)
        }
    }

    function generateIndex (headers, level) {
        var out = ''

        // Header
        if (level == 0) {
            out += '<h2>Contents</h2><dl>'
        } else {
            out += '<ul>'
        }

        // Find the indices of all headers one level below this one
        forEachElement(headers, function (h) {
            return h[0] === 'h' + (level + 1) && h[1].anchor
        }, function (header, subHeaders) {
            // Output header
            if (level == 0) {
                out += '<dt><a href="#' + header[1].anchor + '">' + header[2] + '</a></dt>'
            } else {
                out += '<dd><a href="#' + header[1].anchor + '">' + header[2] + '</a></dd>'
            }
            // Output sub headers (if any)
            if (subHeaders.length) {
                out += generateIndex(subHeaders, level + 1)
            }
        })

        // Footer
        if (level == 0) {
            out += '</dl>'
        } else {
            out += '</ul>'
        }

        return out
    }
    var index = generateIndex(tree.slice(2).filter(function (e) {
        return e[1] && e[1].index
    }), 0)

    function generateBody (elements) {
        function generateElements (elements) {
            return elements.slice(2).map(generateElement).join('')
        }
        function generateElement (element) {
            if (!element) {
                return ''
            } else if (typeof element === 'string') {
                return element
            } else if (element[0] === 'em') {
                return '<i>' + generateElements(element) + '</i>'
            } else if (element[0] === 'strong') {
                return '<b>' + generateElements(element) + '</b>'
            } else if (element[0] === 'code') {
                return '<code>' + generateElements(element) + '</code>'
            } else if (element[0] === 'p') {
                if (element.length === 3 && element[2][0] === 'a' && element[2][2] === 'YouTube') {
                    var id = element[2][1].href
                    return '<a href="https://www.youtube.com/watch?v=' + id+ '" class="thumbnail">' +
                        '<div class="embed-responsive embed-responsive-16by9"><iframe class="embed-responsive-item" ' +
                        'src="https://www.youtube.com/embed/' + id + '" allowfullscreen></iframe></div></a>'
                }
                return '<p>' + generateElements(element) + '</p>'
            } else if (element[0] === 'a') {
                return '<a href=' + element[1].href + '>' + generateElements(element) + '</a>'
            } else if (element[0] === 'ul') {
                return '<ul>' + generateElements(element) + '</ul>'
            } else if (element[0] === 'li') {
                return '<li>' + generateElements(element) + '</li>'
            } else if (element[0] === 'img') {
                return '<img src="' + element[1].src + '" alt="' + element[1].alt + '">'
            } else if (element[0] === 'blockquote') {
                return element.slice(2).map(function (e) {
                    return '<p><mark>' + generateElements(e) + '</mark></p>'
                }).join('')
            } else if (element[0] === 'hr') {
                return '<hr>'
            } else if (element[0] === 'br') {
                return '<br>'
            } else {
                return JSON.stringify(element)
            }
        }
        function generateSection (header, elements) {
            // Remove images and store them for later
            var images = []
            for (var i = 0; i < elements.length; i++) {
                var element = elements[i]
                if (element[0] !== 'p' || element[2][0] !== 'img') break
                images.push(element)
            }
            elements.splice(0, images.length)

            var out = ''
            if (header[1].anchor) {
                out += '<a class="anchor" id="' + header[1].anchor + '"></a>'
            }
            out += '<' + header[0] + ' class="text-ul">' + header[2] + '</' + header[0] +'>'
            out += '<div class="row no-gutter"><div class="col-md-8">'
            // Generate text
            out += elements.map(generateElement).join('')
            out += '</div><div class="col-md-4">'
            // Generate images
            images.forEach(function (image) {
                var url = image[2][1].src
                if (url.endsWith('.jpg') || url.endsWith('.png')) {
                    var fullQualityUrl = url
                    if (url.endsWith('.jpg')) {
                        fullQualityUrl = url.substring(0, url.length - 3) + 'png'
                    }
                    out += '<div class="thumbnail"><a href="' + fullQualityUrl + '" class="thumbnail" data-lightbox="' +
                        url + '"><img src="' +
                        url + '"></a><div class="caption"><p>' + generateElements(image.slice(1)) + '</p></div></div>'
                } else if (url.endsWith('gif')) {
                    var webmUrl = url.substring(0, url.length - 3) + 'webm'
                    out += '<div class="thumbnail">' +
                        '<a href="' + url + '" class="thumbnail" data-lightbox="' +url + '">' +
                        '<video style="display:block;width:100%" autoplay loop src="' + webmUrl + '">' +
                        '<img src="' + url + '">' +
                        '</video></a><div class="caption"><p>' + generateElements(image.slice(1)) + '</p></div></div>'
                } else {
                    console.log('[Warning] Unhandled image format: ' + url)
                }
            })
            out += '</div></div>'
            return out
        }

        var out = ''
        forEachElement(elements, function (e) {
            return e[0] === 'h1'
        }, function (header, elements) {
            out += '<div class="panel panel-default"><div class="panel-body">'
            var firstHeaderIndex = elements.findIndex(function (e) {
                return e[0].match(/h\d/)
            })
            header[0] = 'h2' // h1 headers should be rendered the same way as h2 ones are
            out += generateSection(header, elements.slice(0, firstHeaderIndex === -1 ? elements.length + 1 : firstHeaderIndex))
            forEachElement(elements, function (e) {
                return e[0].match(/h\d/)
            }, function (header, elements) {
                out += generateSection(header, elements)
            })
            out += '</div></div>'
        })
        return out
    }

    var out = fs.readFileSync('template.html', 'utf-8')
        .replace('{{index}}', index)
        .replace('{{content}}', generateBody(tree.slice(2)))
    //out = JSON.stringify(tree, null, 2)
    fs.writeFileSync('content.html', out, 'utf-8')
})
