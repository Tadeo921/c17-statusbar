const fs=require('fs'),vm=require('vm'),assert=require('assert');
const root=process.cwd(), html=fs.readFileSync(root+'/app/src/main/assets/performance-module/webroot/index.html','utf8'),js=fs.readFileSync(root+'/app/src/main/assets/performance-module/webroot/app.js','utf8');
const ids=new Set([...html.matchAll(/id="([^"]+)"/g)].map(m=>m[1]));
let checks=0;
const nodes={}; for(const id of ids)nodes[id]={textContent:'',disabled:false,hidden:false,dataset:{},addEventListener(){},scrollIntoView(){}};
const document={documentElement:{dataset:{}},hidden:false,getElementById(id){assert(ids.has(id),'unknown element '+id);return nodes[id]},querySelector(){return{classList:{toggle(){}}}},addEventListener(){}};
const window={setTimeout(){return 0},clearTimeout(){},addEventListener(){}};
vm.runInNewContext(js,{window,document,console,Date,Set,Promise,Number,Error,Object,String},{filename:'app.js'});
const api=window.C17WebUI;
const fields={installed:'1',pending:'0',disabled:'0',remove:'0',boot:'11111111-1111-1111-1111-111111111111',busy:'0',active_job:'0',module_version:'1.7',module_version_code:'8',record_present:'1',last_state:'current',last_time:'1791858000',last_compiled:'0',last_packages:'2',last_error:'none',last_boot:'11111111-1111-1111-1111-111111111111',last_mode:'auto',last_module_version_code:'8',last_reused:'2',last_verified:'2',last_deferred:'0',last_verification:'verified',last_priority:'background',schedule_kind:'on_demand',schedule_state:'scheduled'};
function raw(f){return'C17_WEB:begin\n'+Object.entries(f).map(([k,v])=>k+'='+v).join('\n')+'\nC17_WEB:end\n'}
function test(patch,title,canRun,canToggle){const parsed=api.parse(raw({...fields,...patch})),view=api.present(parsed,false);assert.equal(view.title,title);assert.equal(view.canRun,canRun);assert.equal(view.canToggle,canToggle);checks+=3;}
test({},'无需编译',true,true);
test({busy:'1',last_state:'running',active_job:'1'},'优化中',false,false);
test({last_state:'running',last_boot:'22222222-2222-2222-2222-222222222222'},'已安装',true,true);
test({last_state:'running',last_module_version_code:'4'},'已安装',true,true);
test({last_state:'waiting',last_mode:'boot',busy:'1'},'等待空闲',false,false);
test({last_state:'waiting',last_mode:'run',busy:'1'},'优化中',false,false);
test({last_state:'running'},'结果待确认',false,false);
test({last_state:'failed',last_error:'compile_cancel_failed',active_job:'1'},'取消未确认',false,false);
test({last_state:'failed',last_error:'compile_cancel_failed',active_job:'0'},'未完成',true,true);
test({last_state:'failed',last_error:'restore_cancel_failed',active_job:'0'},'未完成',true,true);
test({last_state:'failed',last_error:'compile_failed'},'未完成',true,true);
test({last_state:'deferred',last_error:'device_hot'},'已延期',true,true);
for (const reason of ['art_cancel_unavailable','timeout_unavailable']) {
  test({last_state:'deferred',last_error:reason},'编译未开始',true,true);
  assert(!api.present({...fields,last_state:'deferred',last_error:reason},false).message.includes('空闲条件'));checks++;
}
test({disabled:'1'},'已停用',false,true);
test({disabled:'1',pending:'1'},'已停用',false,false);
test({remove:'1',disabled:'1',pending:'1',busy:'1'},'待卸载',false,false);
test({pending:'1',busy:'1'},'待重启',false,false);
test({module_version_code:'4'},'待更新',false,false);
test({busy:'unknown'},'无法确认',false,false);
test({active_job:'unknown'},'无法确认',false,false);
test({active_job:'1'},'取消未确认',false,false);
test({record_present:'0'},'已安装',true,true);
test({installed:'0',pending:'0'},'未安装',false,false);
test({installed:'0',pending:'1'},'待重启',false,false);
for(const payload of [raw(fields).replace('installed=1','installed=x'),raw(fields).replace('last_compiled=0','last_compiled=NaN'),raw(fields).replace('last_compiled=0','last_compiled=-1'),raw(fields).replace('last_mode=auto','last_mode=evil'),raw(fields).replace('last_error=none','last_error=<script>'),raw(fields).replace('C17_WEB:end','installed=1\nC17_WEB:end'),raw(fields).replace('C17_WEB:end',''),raw(fields)+raw(fields),'x'.repeat(16385)]){assert.throws(()=>api.parse(payload));checks++;}
assert(!/\beval\s*\(|new\s+Function\b|\.innerHTML\b|\bfetch\s*\(|XMLHttpRequest|WebSocket/.test(js));checks++;
assert(!/<(?:script|link)[^>]*(?:src|href)="https?:/.test(html));checks++;
assert(nodes.optimize.disabled);checks++;
assert.equal(nodes['status-title'].textContent,'请在 C17 查看');checks++;
assert.equal(api.parse(raw({...fields,last_mode:'uninstall'})).last_mode,'uninstall');checks++;
assert.equal(api.parse(raw({...fields,last_idle_remaining:'600',last_diagnostic:'uuid_missing'})).last_diagnostic,'uuid_missing');checks++;
assert(js.includes('document.hidden || window.c17HostPaused === true'));checks++;
assert(js.includes('c17 ? 50000 : 12000'));checks++;
for(const state of ['scheduled','inactive','unavailable','deferred','blocked','running']){assert(js.includes(state));checks++;}
assert(!js.includes('interval_hours=24'));checks++;
{
  const ledger={total_since:'1792257000',total_compiles:'3',total_wall_ms:'12500',total_cpu_ms:'900',total_size_kb:'6144',total_reuses:'8',total_avoided_ms:'30000',runtime_sampled:'1792257900',runtime_jit_ms:'6000',runtime_proc_s:'600',runtime_aot:'1'};
  const view=api.benefitView(api.parse(raw({...fields,...ledger})));
  assert.equal(view.compiles,'3 次');assert.equal(view.saved,'8 次');assert.equal(view.aot,'已使用');assert.equal(view.jit,'600 ms/分');
  assert(view.compilesNote.includes('13 秒')&&view.compilesNote.includes('6 MB'));assert(view.savedNote.includes('30 秒')&&view.savedNote.includes('估算'));
  assert(view.jitNote.includes('10 分钟'));checks+=7;
  const empty=api.benefitView(api.parse(raw(fields)));
  assert.equal(empty.compiles,'—');assert.equal(empty.aot,'—');assert.equal(empty.jit,'—');assert.equal(empty.since,'尚无记录');checks+=4;
  assert.equal(api.benefitView(api.parse(raw({...fields,...ledger,runtime_proc_s:'30'}))).jit,'—');checks++;
  assert.equal(api.benefitView(api.parse(raw({...fields,...ledger,runtime_aot:'0'}))).aot,'未使用');checks++;
  assert.equal(api.benefitView(api.parse(raw({...fields,total_since:'x',total_compiles:'-4'}))).compiles,'—');checks++;
  const ring=api.ringView(api.parse(raw(fields)),true);assert.equal(ring.label,'2/2');assert(ring.dash.startsWith('213.6'));assert.equal(ring.opacity,'1');checks+=3;
  assert.equal(api.ringView(api.parse(raw({...fields,last_verified:'1'})),false).opacity,'0.55');checks++;
  assert.equal(api.ringView(api.parse(raw({...fields,record_present:'0'})),false).label,'—');checks++;
  assert.equal(api.parse(raw({...fields,last_diagnostic:'client_failed'})).last_diagnostic,'client_failed');checks++;
}
if(process.argv[2]){const real=api.parse(fs.readFileSync(process.argv[2],'utf8'));assert.equal(real.module_version_code,'8');assert.equal(real.last_state,'current');checks+=2;}
console.log('WebUI protocol and safety: '+checks+' assertions passed; no Root/device operations.');
