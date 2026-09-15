import http from 'k6/http';
import { check, sleep } from 'k6';
export const options={vus:5,duration:'30s',thresholds:{http_req_failed:['rate<0.01'],http_req_duration:['p(95)<1000'],http_reqs:['rate>2']}};
const base=__ENV.BASE_URL||'http://localhost:8080';
if(!/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(base)) throw new Error('Safety guard: this script only targets localhost');
export function setup(){const r=http.post(`${base}/api/auth/login`,JSON.stringify({username:'demo.user',password:__ENV.DEMO_PASSWORD||'NovaBankDemo!2026'}),{headers:{'Content-Type':'application/json'}});check(r,{'login succeeds':x=>x.status===200});return {token:r.json('accessToken')};}
export default function(data){const h={headers:{Authorization:`Bearer ${data.token}`}};['/api/customers/me','/api/accounts','/api/transactions','/api/cards','/api/payments','/api/statements','/api/notifications'].forEach(path=>check(http.get(base+path,h),{[`${path} succeeds`]:r=>r.status===200}));sleep(1);}
