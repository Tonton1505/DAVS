import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || "admin@davs.local";
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || "Admin@123456!ChangeMe";
const DASHBOARD_ID = "00000000-0000-0000-0000-000000000501";
const DATASOURCE_ID = "00000000-0000-0000-0000-000000000101";

export const options = {
  stages: [
    { duration: "15s", target: 100 },
    { duration: "30s", target: 500 },
    { duration: "15s", target: 0 },
  ],
  thresholds: {
    "http_req_duration{endpoint:login}": ["p(95)<3000"],
    "http_req_duration{endpoint:dashboard_view}": ["p(95)<5000"],
    "http_req_duration{endpoint:dashboard_filter}": ["p(95)<3000"],
    "http_req_duration{endpoint:admin_list}": ["p(95)<2000"],
    http_req_failed: ["rate<0.01"],
  },
};

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
    { headers: { "Content-Type": "application/json" }, tags: { endpoint: "login" } },
  );
  check(res, { "login status 200": (r) => r.status === 200 });
  const body = res.json();
  return { token: body ? body.accessToken : "" };
}

export default function (data) {
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${data.token}`,
  };

  // NFR-PERF-02: Open dashboard <= 5s
  const viewRes = http.get(`${BASE_URL}/api/v1/dashboards/${DASHBOARD_ID}/view`, {
    headers,
    tags: { endpoint: "dashboard_view" },
  });
  check(viewRes, { "dashboard view ok": (r) => r.status === 200 });

  // NFR-PERF-03: Filter / interactive query <= 3s
  const queryRes = http.post(
    `${BASE_URL}/api/v1/query`,
    JSON.stringify({
      datasourceId: DATASOURCE_ID,
      columns: ["Vung"],
      rows: ["SUM(Doanh số)"],
      limit: 12,
    }),
    { headers, tags: { endpoint: "dashboard_filter" } },
  );
  check(queryRes, { "query ok": (r) => r.status === 200 });

  // NFR-PERF-04: Admin list <= 2s
  const adminRes = http.get(`${BASE_URL}/api/v1/audit-logs?page=0&size=25`, {
    headers,
    tags: { endpoint: "admin_list" },
  });
  check(adminRes, { "admin list ok": (r) => r.status === 200 });

  sleep(0.5);
}
