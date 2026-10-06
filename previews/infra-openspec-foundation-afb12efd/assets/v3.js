
(()=>{const box=document.querySelector('#module-search');if(!box)return;
const items=[...document.querySelectorAll('.module')],status=document.querySelector('#search-count');
const normal=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
box.addEventListener('input',()=>{const terms=normal(box.value).trim().split(/\s+/).filter(Boolean);let n=0;
items.forEach(el=>{el.hidden=!terms.every(t=>normal(el.textContent).includes(t));if(!el.hidden)n++});
document.querySelectorAll('section').forEach(s=>{const rows=s.querySelectorAll('.module');if(rows.length)s.hidden=[...rows].every(r=>r.hidden)});
status.textContent=n+' de '+items.length+' módulos exibidos';});})();
