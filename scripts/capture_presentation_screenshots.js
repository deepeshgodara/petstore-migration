const puppeteer = require('/Users/deepeshgodara/Documents/petstore1.3.1_02/petstore-frontend/node_modules/puppeteer-core');
const path = require('path');
const fs = require('fs');

const CHROME_PATH = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const OUT_DIR = '/Users/deepeshgodara/Documents/petstore1.3.1_02/docs/demo_screenshots';
const ARTIFACT_DIR = '/Users/deepeshgodara/.gemini/antigravity-ide/brain/3be3e741-1f0b-468f-aa65-d94d89e6b6de';

fs.mkdirSync(OUT_DIR, { recursive: true });

async function run() {
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1440,900']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900 });

  // 1. Storefront Home
  console.log('Capturing Storefront Home...');
  await page.goto('http://localhost:3000/', { waitUntil: 'networkidle0' });
  await page.waitForSelector('.product-grid', { timeout: 10000 }).catch(() => {});
  const p1 = path.join(OUT_DIR, '01_storefront_home.png');
  await page.screenshot({ path: p1 });
  fs.copyFileSync(p1, path.join(ARTIFACT_DIR, '01_storefront_home.png'));

  // 2. Open Login / Registration Modal
  console.log('Capturing User Registration Modal...');
  const userBtn = await page.$('button[title="Account profile"]');
  if (userBtn) {
    await userBtn.click();
    await new Promise(r => setTimeout(r, 600));
    // Click "Create Account" tab
    const tabs = await page.$$('button');
    for (const tab of tabs) {
      const text = await (await tab.getProperty('innerText')).jsonValue();
      if (text.includes('Create Account')) {
        await tab.click();
        await new Promise(r => setTimeout(r, 400));
        break;
      }
    }
  }
  const p2 = path.join(OUT_DIR, '02_user_registration_modal.png');
  await page.screenshot({ path: p2 });
  fs.copyFileSync(p2, path.join(ARTIFACT_DIR, '02_user_registration_modal.png'));

  // 3. Ops Parity Dashboard (as engineer)
  console.log('Capturing Ops Parity Dashboard...');
  await page.evaluate(() => {
    localStorage.setItem('petstore_auth_user', JSON.stringify({
      username: 'engineer',
      name: 'Data Reliability Engineer',
      email: 'sre@petstore.internal',
      role: 'ROLE_ENGINEER',
      token: 'jwt_mock_token_sre_parity'
    }));
  });
  await page.goto('http://localhost:3000/ops', { waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 1000));
  const p3 = path.join(OUT_DIR, '03_ops_parity_dashboard.png');
  await page.screenshot({ path: p3 });
  fs.copyFileSync(p3, path.join(ARTIFACT_DIR, '03_ops_parity_dashboard.png'));

  // 4. Admin Dashboard (as admin)
  console.log('Capturing Admin Dashboard...');
  await page.evaluate(() => {
    localStorage.setItem('petstore_auth_user', JSON.stringify({
      username: 'admin',
      name: 'Store Operations Administrator',
      email: 'admin@petstore.internal',
      role: 'ROLE_ADMIN',
      token: 'jwt_mock_token_admin_ops'
    }));
  });
  await page.goto('http://localhost:3000/admin', { waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 1000));
  const p4 = path.join(OUT_DIR, '04_admin_dashboard_analytics.png');
  await page.screenshot({ path: p4 });
  fs.copyFileSync(p4, path.join(ARTIFACT_DIR, '04_admin_dashboard_analytics.png'));

  // 5. Supplier Portal (as supplier)
  console.log('Capturing Supplier Portal...');
  await page.evaluate(() => {
    localStorage.setItem('petstore_auth_user', JSON.stringify({
      username: 'supplier',
      name: 'Acme Pet Supply Co.',
      email: 'supplier@petstore.internal',
      role: 'ROLE_SUPPLIER',
      token: 'jwt_mock_token_supplier_inventory'
    }));
  });
  await page.goto('http://localhost:3000/supplier', { waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 1000));
  const p5 = path.join(OUT_DIR, '05_supplier_portal.png');
  await page.screenshot({ path: p5 });
  fs.copyFileSync(p5, path.join(ARTIFACT_DIR, '05_supplier_portal.png'));

  await browser.close();
  console.log('All 5 screenshots captured successfully!');
}

run().catch(console.error);
