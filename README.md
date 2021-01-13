
(function () {

    // 기본 function 정의

    'use strict'

    const refreshTime = 175 // 새로고침 시간

    function routine(){
        location.reload()
    }

    var styles$4 = {"loader":"Refresher-module_loader__2EBKq","light":"Refresher-module_light__2hVGO","loaderspin":"Refresher-module_loaderspin__3kEgT"};
    var stylesheet$4="@keyframes Refresher-module_light__2hVGO{\r\n    0% {\r\n        background-color: rgb(246, 247, 239);\r\n    }\r\n    100% {\r\n        background-color: rgba(255, 255, 255, 0);\r\n    }\r\n\r\n}\r\n\r\n@keyframes Refresher-module_loaderspin__3kEgT {\r\n    0% { transform: rotate(0deg);\r\n        box-shadow: 0 0 15px #3d414d;\r\n    }\r\n    5% {\r\n        box-shadow: 0 0 -10px #3d414d;\r\n    }\r\n    15%{\r\n        box-shadow: 0 0 0px #3d414d;\r\n    }\r\n    100% { transform: rotate(360deg);\r\n        box-shadow: 0 0 0px #3d414d;\r\n    }\r\n}\r\n\r\n.Refresher-module_loader__2EBKq {\r\n    border: 6px solid #d3d3d3;\r\n    border-top: 6px solid #3d414d;\r\n    border-radius: 50%;\r\n    position: fixed;\r\n    bottom: 30px;\r\n    left: 10px;\r\n    width: 40px;\r\n    height: 40px;\r\n    z-index: 20;\r\n}";

    function initLoader() {
        document.head.append(VM.createElement("style", null, stylesheet$4));
        const loader = VM.createElement("div", {
            id: "article_loader",
            class: styles$4.loader
        });
        document.body.append(loader);
        return loader;
    }
    let loader = initLoader();

    function playLoader(loader, time) {
        if (loader) {
            loader.removeAttribute('style');
            setTimeout(() => {
                loader.setAttribute('style', `animation: ${styles$4.loaderspin} ${time}s ease-in-out`);
            }, 50);
        }
    }

    let loadLoop = null;
    loadLoop = setInterval(routine, refreshTime * 1000);

    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            clearInterval(loadLoop);
            loadLoop = null;
        } else {
            if (loadLoop == null) {
                playLoader(loader, refreshTime);
                loadLoop = setInterval(routine, refreshTime * 1000);
            }
        }
    });
    document.addEventListener('click', event => {
        if (event.target.tagName != 'INPUT') return;

        const allChk = document.querySelector('#comment_chk_all')

        if (allChk.checked) {
            if (allChk.checked) {
                clearInterval(loadLoop);
                loadLoop = null;
            } else {
                playLoader(loader, refreshTime);
                loadLoop = setInterval(routine, refreshTime * 1000);
            }
        } else {
            const btns = document.querySelectorAll('.article_chkbox');

            for (const btn of btns) {
                if (btn.checked) {
                    clearInterval(loadLoop)
                    loadLoop = null
                    return
                }
            }
            playLoader(loader, refreshTime);
            loadLoop = setInterval(routine, refreshTime * 1000);
        }
    });
    // 기본 function 정의 끝
  
  function newTestCheck(){
      let recent = document.querySelectorAll('ul')[10].querySelectorAll('li')[1]
      let recentTextLi = recent.querySelector('.list__title')
      let recentText = recentTextLi.innerText
      //let recentReply = recent.querySelectorAll('.col-title')[15].innerText
      
      let matchText = "1월 13일(수) 임시 점검 안내"
      //let matchReply = "매크로"

      let matchResult = false
      if(matchText == recentText) { 
        matchResult = true 
      }
      //let matchReplyResult = new RegExp(matchReply).test(recentReply)
      
      if(!matchResult){
        recent.style.backgroundColor='#d9534f'
        for(let i=0; i<20; i++){
          beep(); 
        }
        return
      }
      
//       if(!matchReplyResult){
//         recent.style.backgroundColor='#5cb85c'
//         for(let i=0; i<20; i++){
//           beep(); 
//         }
//       }
      
    }

    function beep() { 
      let snd = new Audio("data:audio/wav;base64,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qJrx4KOQjahZxWbcZgztj2c49nKmkId44S71j0c8eV9yDK6uPRzx5X18eDvjvQ6yKo9ZSS6l//8elePK/Lf//IInrOF/FvDoADYAGBMGb7FtErm5MXMlmPAJQVgWta7Zx2go+8xJ0UiCb8LHHdftWyLJE0QIAIsI+UbXu67dZMjmgDGCGl1H+vpF4NSDckSIkk7Vd+sxEhBQMRU8j/12UIRhzSaUdQ+rQU5kGeFxm+hb1oh6pWWmv3uvmReDl0UnvtapVaIzo1jZbf/pD6ElLqSX+rUmOQNpJFa/r+sa4e/pBlAABoAAAAA3CUgShLdGIxsY7AUABPRrgCABdDuQ5GC7DqPQCgbbJUAoRSUj+NIEig0YfyWUho1VBBBA//uQZB4ABZx5zfMakeAAAAmwAAAAF5F3P0w9GtAAACfAAAAAwLhMDmAYWMgVEG1U0FIGCBgXBXAtfMH10000EEEEEECUBYln03TTTdNBDZopopYvrTTdNa325mImNg3TTPV9q3pmY0xoO6bv3r00y+IDGid/9aaaZTGMuj9mpu9Mpio1dXrr5HERTZSmqU36A3CumzN/9Robv/Xx4v9ijkSRSNLQhAWumap82WRSBUqXStV/YcS+XVLnSS+WLDroqArFkMEsAS+eWmrUzrO0oEmE40RlMZ5+ODIkAyKAGUwZ3mVKmcamcJnMW26MRPgUw6j+LkhyHGVGYjSUUKNpuJUQoOIAyDvEyG8S5yfK6dhZc0Tx1KI/gviKL6qvvFs1+bWtaz58uUNnryq6kt5RzOCkPWlVqVX2a/EEBUdU1KrXLf40GoiiFXK///qpoiDXrOgqDR38JB0bw7SoL+ZB9o1RCkQjQ2CBYZKd/+VJxZRRZlqSkKiws0WFxUyCwsKiMy7hUVFhIaCrNQsKkTIsLivwKKigsj8XYlwt/WKi2N4d//uQRCSAAjURNIHpMZBGYiaQPSYyAAABLAAAAAAAACWAAAAApUF/Mg+0aohSIRobBAsMlO//Kk4soosy1JSFRYWaLC4qZBYWFRGZdwqKiwkNBVmoWFSJkWFxX4FFRQWR+LsS4W/rFRb/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////VEFHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAU291bmRib3kuZGUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMjAwNGh0dHA6Ly93d3cuc291bmRib3kuZGUAAAAAAAAAACU=");
      snd.play(); 
    } 


    // 스크립트 시작
    playLoader(loader, refreshTime) // 로더 애니메이션 로드
    newTestCheck()

}());