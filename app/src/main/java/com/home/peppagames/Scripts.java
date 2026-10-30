package com.home.peppagames;

/** JavaScript injected into nick.com pages. */
final class Scripts {
    private Scripts() {
    }

    /**
     * Runs on the Peppa games list page. Scrolls to trigger lazy loading and returns
     * [{url, title, img}] for every link to a nick.com game page.
     */
    static final String FIND_GAMES = "(function(){"
            + "try{window.scrollTo(0,document.body?document.body.scrollHeight:0);}catch(e){}"
            + "var here=location.pathname.replace(/\\/+$/,'');"
            + "var byUrl={},out=[];"
            + "function abs(s){try{return new URL(s,location.href).href;}catch(e){return '';}}"
            + "function pickImg(a){"
            + "  var im=a.querySelector('img');"
            + "  if(im){var s=im.currentSrc||im.getAttribute('src')||im.getAttribute('data-src')||'';"
            + "    if((!s||s.indexOf('data:')===0)&&im.getAttribute('srcset'))"
            + "      s=im.getAttribute('srcset').split(',').pop().trim().split(' ')[0];"
            + "    if(s&&s.indexOf('data:')!==0)return abs(s);}"
            + "  var els=[a].concat([].slice.call(a.querySelectorAll('*')));"
            + "  for(var i=0;i<els.length;i++){var bg=getComputedStyle(els[i]).backgroundImage;"
            + "    var m=bg&&bg.match(/url\\([\"']?([^\"')]+)/);if(m)return abs(m[1]);}"
            + "  return '';}"
            + "[].slice.call(document.querySelectorAll('a[href]')).forEach(function(a){"
            + "  var u;try{u=new URL(a.getAttribute('href'),location.href);}catch(e){return;}"
            + "  if(!/(^|\\.)nick\\.com$/.test(u.hostname))return;"
            + "  var p=u.pathname.replace(/\\/+$/,'');"
            + "  if(!/^\\/games\\/[^\\/]+/.test(p)||p===here)return;"
            + "  var url=u.origin+p;"
            + "  var im=a.querySelector('img');"
            + "  var title=(a.getAttribute('aria-label')||a.getAttribute('title')||(im&&im.alt)||a.textContent||'')"
            + "    .replace(/\\s+/g,' ').trim();"
            + "  if(!title||title.length>60)title=decodeURIComponent(p.split('/').pop()).replace(/[-_]+/g,' ').replace(/\\.html?$/,'');"
            + "  var img=pickImg(a);"
            + "  var g=byUrl[url];"
            + "  if(g){if(!g.img&&img)g.img=img;return;}"
            + "  g={url:url,title:title,img:img};byUrl[url]=g;out.push(g);"
            + "});"
            + "return out;"
            + "})()";

    /**
     * Runs on a game page. Finds the biggest iframe/canvas/video, stretches it over the
     * whole screen and hides everything else on the page (headers, ads, footers).
     */
    static final String FOCUS_GAME = "(function(){"
            + "var M='data-pp-game';"
            + "function area(e){var r=e.getBoundingClientRect();return r.width*r.height;}"
            + "var best=null,ba=0;"
            + "[].slice.call(document.querySelectorAll('iframe,canvas,embed,object,video')).forEach(function(e){"
            + "  var a=area(e);if(a>ba){ba=a;best=e;}});"
            + "var cur=document.querySelector('['+M+']');"
            + "if(!best||ba<40000)return cur?'kept':'none';"
            + "if(cur&&cur!==best){cur.removeAttribute(M);"
            + "  [].slice.call(document.querySelectorAll('[data-pp-anc]')).forEach(function(e){e.removeAttribute('data-pp-anc');});}"
            + "best.setAttribute(M,'1');"
            + "for(var p=best.parentElement;p;p=p.parentElement)p.setAttribute('data-pp-anc','1');"
            + "if(!document.getElementById('pp-style')){var s=document.createElement('style');s.id='pp-style';"
            + "  s.textContent='html,body{overflow:hidden!important;background:#000!important;margin:0!important}'"
            + "  +'body *{visibility:hidden!important}'"
            + "  +'['+M+'],['+M+'] *{visibility:visible!important}'"
            + "  +'[data-pp-anc]{transform:none!important;filter:none!important;contain:none!important;'"
            + "  +'will-change:auto!important;perspective:none!important}'"
            + "  +'['+M+']{position:fixed!important;left:0!important;top:0!important;right:auto!important;bottom:auto!important;'"
            + "  +'width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;'"
            + "  +'margin:0!important;padding:0!important;border:0!important;z-index:2147483647!important;'"
            + "  +'background:#000!important;object-fit:contain!important;transform:none!important}';"
            + "  (document.head||document.documentElement).appendChild(s);}"
            + "return 'focused:'+best.tagName;"
            + "})()";
}
