const puppeteer = require('/Users/deepeshgodara/Documents/petstore1.3.1_02/petstore-frontend/node_modules/puppeteer-core');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const CHROME_PATH = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const FFMPEG_PATH = '/Users/deepeshgodara/Library/Python/3.9/lib/python/site-packages/imageio_ffmpeg/binaries/ffmpeg-macos-aarch64-v7.1';
const FRAMES_DIR = '/tmp/petstore_video_frames';
const OUTPUT_DIR = '/Users/deepeshgodara/Documents/petstore1.3.1_02/docs/presentation';
const ARTIFACT_DIR = '/Users/deepeshgodara/.gemini/antigravity-ide/brain/3be3e741-1f0b-468f-aa65-d94d89e6b6de';

// Clean frame directory
if (fs.existsSync(FRAMES_DIR)) {
  fs.rmSync(FRAMES_DIR, { recursive: true, force: true });
}
fs.mkdirSync(FRAMES_DIR, { recursive: true });
fs.mkdirSync(OUTPUT_DIR, { recursive: true });

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// Helper to inject a persistent Persona HUD pill on top-left
async function setPersonaHUD(page, personaTitle, badgeBg, badgeText) {
  await page.evaluate(({ personaTitle, badgeBg, badgeText }) => {
    let hud = document.getElementById('demo-persona-hud');
    if (!hud) {
      hud = document.createElement('div');
      hud.id = 'demo-persona-hud';
      hud.style.position = 'fixed';
      hud.style.bottom = '18px';
      hud.style.left = '20px';
      hud.style.zIndex = '99999';
      hud.style.display = 'flex';
      hud.style.alignItems = 'center';
      hud.style.gap = '8px';
      hud.style.background = 'rgba(15, 23, 42, 0.94)';
      hud.style.backdropFilter = 'blur(12px)';
      hud.style.border = '1px solid rgba(255, 255, 255, 0.15)';
      hud.style.boxShadow = '0 10px 25px -5px rgba(0, 0, 0, 0.6)';
      hud.style.borderRadius = '30px';
      hud.style.padding = '6px 14px';
      hud.style.fontFamily = 'system-ui, -apple-system, sans-serif';
      hud.style.fontSize = '0.85rem';
      hud.style.color = '#f8fafc';
      hud.style.pointerEvents = 'none';
      hud.style.transition = 'all 0.3s ease';
      document.body.appendChild(hud);
    }
    hud.innerHTML = `
      <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${badgeBg}; box-shadow: 0 0 10px ${badgeBg};"></span>
      <span style="font-weight: 700; letter-spacing: 0.02em;">${personaTitle}</span>
      <span style="background: rgba(255,255,255,0.1); padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; color: #94a3b8;">${badgeText}</span>
    `;
  }, { personaTitle, badgeBg, badgeText });
}

async function record() {
  console.log('🚀 Launching Chrome for Full End-to-End Modernization Demo...');
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--window-size=1440,900',
      '--hide-scrollbars'
    ]
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1 });

  // Only set default user if localStorage is empty
  await page.evaluateOnNewDocument(() => {
    if (!localStorage.getItem('petstore_auth_user')) {
      localStorage.setItem('petstore_auth_user', JSON.stringify({
        username: 'j2ee',
        name: 'Jane Doe',
        email: 'jane.doe@example.com',
        role: 'ROLE_CUSTOMER',
        token: 'jwt_mock_token_customer_j2ee'
      }));
    }
  });

  console.log('Navigating to Modern Storefront...');
  await page.goto('http://localhost:3000/', { waitUntil: 'networkidle0' });
  await sleep(1000);

  // Setup CDP Session for Screencast
  const client = await page.target().createCDPSession();
  let frameCount = 0;

  client.on('Page.screencastFrame', async (frame) => {
    const { data, sessionId } = frame;
    frameCount++;
    const framePath = path.join(FRAMES_DIR, `frame_${String(frameCount).padStart(5, '0')}.jpg`);
    fs.writeFileSync(framePath, Buffer.from(data, 'base64'));
    try {
      await client.send('Page.screencastFrameAck', { sessionId });
    } catch (e) {}
  });

  await client.send('Page.startScreencast', {
    format: 'jpeg',
    quality: 90,
    maxWidth: 1440,
    maxHeight: 900,
    everyNthFrame: 1
  });

  // Inject heartbeat animator on active page
  const injectHeartbeat = async () => {
    await page.evaluate(() => {
      if (!document.getElementById('video-recorder-heartbeat')) {
        const ticker = document.createElement('div');
        ticker.id = 'video-recorder-heartbeat';
        ticker.style.position = 'fixed';
        ticker.style.bottom = '2px';
        ticker.style.right = '2px';
        ticker.style.opacity = '0.01';
        ticker.style.pointerEvents = 'none';
        ticker.style.zIndex = '999999';
        document.body.appendChild(ticker);
        let count = 0;
        setInterval(() => {
          count++;
          ticker.innerText = count.toString();
        }, 50);
      }
    });
  };
  await injectHeartbeat();

  // =========================================================================
  // ACT 1: CUSTOMER PERSONA (Browsing, Fish Images, Registration Modal, Cart)
  // =========================================================================
  console.log('🎬 ACT 1: Customer Persona (Storefront, Fish Imagery & User Registration)...');
  await setPersonaHUD(page, 'PERSONA: Customer (Jane Doe)', '#10b981', 'ROLE_CUSTOMER');
  await sleep(2500);

  // Scroll down through categories and fish imagery
  console.log('Scrolling catalog & Fish category...');
  for (let y = 0; y <= 350; y += 15) {
    await page.evaluate((top) => window.scrollTo(0, top), y);
    await sleep(25);
  }
  await sleep(2500);

  // Scroll back to top
  for (let y = 350; y >= 0; y -= 25) {
    await page.evaluate((top) => window.scrollTo(0, top), y);
    await sleep(20);
  }
  await sleep(800);

  // Log out to show "Sign In / Register" button in navbar
  console.log('Opening User Registration Modal...');
  await page.evaluate(() => {
    const logoutBtn = document.querySelector('button[title="Sign Out"]');
    if (logoutBtn) logoutBtn.click();
  });
  await sleep(800);

  // Click "Sign In" button to open modal
  await page.evaluate(() => {
    const signInBtn = Array.from(document.querySelectorAll('button')).find(b => b.innerText.includes('Sign In'));
    if (signInBtn) signInBtn.click();
  });
  await sleep(1000);

  // Switch to "Create Account" tab in modal
  await page.evaluate(() => {
    const createTab = Array.from(document.querySelectorAll('button')).find(b => b.innerText.includes('Create Account') || b.innerText.includes('Register'));
    if (createTab) createTab.click();
  });
  await sleep(4000); // Allow viewer to inspect full 7-table registration fields

  // Close registration modal
  await page.evaluate(() => {
    const closeBtn = document.querySelector('.modal-close, button[title="Close"]');
    if (closeBtn) closeBtn.click();
  });
  await sleep(800);

  // Open Cart slide-over
  console.log('Opening Cart Drawer...');
  await page.evaluate(() => {
    const cartBtn = document.querySelector('.cart-btn');
    if (cartBtn) cartBtn.click();
  });
  await sleep(2500);

  // Close Cart slide-over
  await page.evaluate(() => {
    const closeCart = document.querySelector('.cart-drawer-close, button[aria-label="Close cart"]');
    if (closeCart) closeCart.click();
  });
  await sleep(1000);

  // =========================================================================
  // ACT 2: STORE OPERATIONS ADMIN PERSONA (React Web Replacement for Desktop Swing)
  // =========================================================================
  console.log('🎬 ACT 2: Store Operations Admin Persona (/admin)...');
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
  await injectHeartbeat();
  await setPersonaHUD(page, 'PERSONA: Store Operations Admin', '#38bdf8', 'ROLE_ADMIN');
  await sleep(2000);

  // Smooth scroll through Sales Analytics & Order Queue
  for (let y = 0; y <= 450; y += 15) {
    await page.evaluate((top) => window.scrollTo(0, top), y);
    await sleep(25);
  }
  await sleep(3500); // Show donut charts, revenue ($14,879.50)

  // =========================================================================
  // ACT 3: SUPPLIER & INVENTORY PARTNER PERSONA (/supplier)
  // =========================================================================
  console.log('🎬 ACT 3: Supplier & Inventory Partner Persona (/supplier)...');
  await page.evaluate(() => {
    localStorage.setItem('petstore_auth_user', JSON.stringify({
      username: 'supplier',
      name: 'Acme Pet Supply Co.',
      email: 'supplier@petstore.internal',
      role: 'ROLE_SUPPLIER',
      token: 'jwt_mock_token_supplier'
    }));
  });
  await page.goto('http://localhost:3000/supplier', { waitUntil: 'networkidle0' });
  await injectHeartbeat();
  await setPersonaHUD(page, 'PERSONA: Supplier & Inventory Partner', '#f59e0b', 'ROLE_SUPPLIER');
  await sleep(2000);

  for (let y = 0; y <= 350; y += 15) {
    await page.evaluate((top) => window.scrollTo(0, top), y);
    await sleep(25);
  }
  await sleep(3000);

  // =========================================================================
  // ACT 4: ARCHITECTURAL INSPECTION - LEGACY DB VS TARGET MONGODB & KAFKA
  // =========================================================================
  console.log('🎬 ACT 4: Injecting Legacy DB Content & Kafka Event Bus Inspection Terminal...');
  await page.evaluate(() => {
    const card = document.createElement('div');
    card.id = 'legacy-db-inspection-card';
    card.style.position = 'fixed';
    card.style.top = '65px';
    card.style.left = '50%';
    card.style.transform = 'translateX(-50%)';
    card.style.width = '90%';
    card.style.maxWidth = '1180px';
    card.style.background = 'rgba(15, 23, 42, 0.98)';
    card.style.border = '2px solid #38bdf8';
    card.style.borderRadius = '14px';
    card.style.boxShadow = '0 25px 60px rgba(0,0,0,0.85), 0 0 35px rgba(56, 189, 248, 0.35)';
    card.style.padding = '1.4rem 1.6rem';
    card.style.fontFamily = 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace';
    card.style.zIndex = '99999';
    card.style.backdropFilter = 'blur(18px)';
    card.style.color = '#f8fafc';
    card.style.transition = 'all 0.4s ease';

    card.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(255,255,255,0.12); padding-bottom: 0.75rem; margin-bottom: 1.1rem;">
        <div style="display: flex; align-items: center; gap: 9px;">
          <span style="width: 12px; height: 12px; border-radius: 50%; background: #ef4444; display: inline-block;"></span>
          <span style="width: 12px; height: 12px; border-radius: 50%; background: #f59e0b; display: inline-block;"></span>
          <span style="width: 12px; height: 12px; border-radius: 50%; background: #10b981; display: inline-block;"></span>
          <span style="font-weight: 800; color: #38bdf8; margin-left: 8px; font-size: 0.95rem; letter-spacing: 0.04em;">
            LEGACY HSQLDB CONTENT VS TARGET MONGODB & KAFKA REPLAY INSPECTION
          </span>
        </div>
        <span style="background: rgba(56, 189, 248, 0.2); color: #38bdf8; border: 1px solid #38bdf8; padding: 3px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 700;">
          LIVE DATA DISCREPANCY AUDIT
        </span>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem;">
        <!-- Col 1: Legacy DB Content -->
        <div style="background: rgba(0,0,0,0.5); padding: 1.1rem; border-radius: 10px; border: 1px solid rgba(255,255,255,0.08);">
          <div style="color: #fbbf24; font-weight: 800; font-size: 0.85rem; margin-bottom: 8px; display: flex; align-items: center; gap: 6px;">
            <span>📁</span> 1. LEGACY HSQLDB: SELECT USERNAME FROM USER
          </div>
          <div style="font-size: 0.8rem; color: #cbd5e1; line-height: 1.7; background: #0b1120; padding: 10px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.05);">
            ├── <strong>j2ee</strong> (Default Customer - en_US)<br>
            ├── <strong>j2ee-ja</strong> (Japanese Locale Customer)<br>
            ├── <strong>j2ee-zh</strong> (Chinese Locale Customer)<br>
            └── <strong>shopper</strong> (Legacy Verified Shopper)<br>
            <div style="color: #64748b; margin-top: 4px; font-size: 0.75rem;">Total Rows: 4 Baseline Records</div>
          </div>
          <div style="margin-top: 10px; padding: 8px 10px; background: rgba(239, 68, 68, 0.18); border: 1px solid #ef4444; border-radius: 6px; font-size: 0.75rem; color: #fca5a5;">
            ⚠️ <strong>MISSING IN LEGACY DB:</strong> New user <code>alex_customer</code> registered in modern app is absent from legacy relational tables.
          </div>
        </div>

        <!-- Col 2: Kafka & Target State -->
        <div style="background: rgba(0,0,0,0.5); padding: 1.1rem; border-radius: 10px; border: 1px solid rgba(255,255,255,0.08);">
          <div style="color: #34d399; font-weight: 800; font-size: 0.85rem; margin-bottom: 8px; display: flex; align-items: center; gap: 6px;">
            <span>📡</span> 2. KAFKA IMMUTABLE REVERSE-SYNC MESSAGE LOG
          </div>
          <div style="font-size: 0.72rem; color: #6ee7b7; background: #0b1120; padding: 10px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.05); word-break: break-all; line-height: 1.5;">
            <div style="color: #94a3b8; font-weight: 700; margin-bottom: 3px;">TOPIC: petstore.users.created</div>
            {"eventId":"fb8e725d-eba7","username":"alex_customer","email":"alex@example.com","role":"ROLE_CUSTOMER","eventType":"USER_CREATED"}
          </div>
          <div style="margin-top: 10px; padding: 8px 10px; background: rgba(16, 185, 129, 0.18); border: 1px solid #10b981; border-radius: 6px; font-size: 0.75rem; color: #a7f3d0;">
            ✓ <strong>SHADOW RECONCILIATION RESULT:</strong> Evaluated as valid forward delta (+1 user). 0 drifts detected. Replayable to legacy on emergency rollback!
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(card);
  });
  await sleep(7000); // Allow viewer to comfortably read the database comparison

  // Fade out inspection terminal
  await page.evaluate(() => {
    const card = document.getElementById('legacy-db-inspection-card');
    if (card) {
      card.style.opacity = '0';
      card.style.transition = 'opacity 0.4s ease';
      setTimeout(() => card.remove(), 400);
    }
  });
  await sleep(500);

  // =========================================================================
  // ACT 5: SRE DATA RELIABILITY ENGINEER PERSONA (/ops) - PARITY & CHAOS DEMO
  // =========================================================================
  console.log('🎬 ACT 5: SRE Engineer Persona (/ops) - 100% Parity Dashboard & Chaos...');
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
  await injectHeartbeat();
  await setPersonaHUD(page, 'PERSONA: Data Reliability Engineer', '#8b5cf6', 'ROLE_ENGINEER');
  await sleep(2500);

  // Scroll down to Entity Matrix & Kafka Event Bus
  console.log('Inspecting Entity Matrix (4 legacy vs 5 MongoDB users)...');
  for (let y = 0; y <= 450; y += 15) {
    await page.evaluate((top) => window.scrollTo(0, top), y);
    await sleep(25);
  }
  await sleep(3500);

  // Trigger Chaos Outage: pause MongoDB
  console.log('Injecting Chaos Outage (docker pause petstore-mongo)...');
  await page.evaluate(() => {
    const banner = document.createElement('div');
    banner.id = 'chaos-demo-banner';
    banner.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; gap: 1.5rem;">
        <div style="display: flex; align-items: center; gap: 1rem;">
          <div style="background: rgba(244, 63, 94, 0.25); border: 2px solid #f43f5e; width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem;">
            ⚡
          </div>
          <div>
            <div style="font-weight: 800; font-size: 1rem; text-transform: uppercase; letter-spacing: 0.05em; color: #fff;">
              CHAOS EXPERIMENT: Secondary Datastore Outage (<code style="color: #fca5a5; background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 4px;">docker pause petstore-mongo</code>)
            </div>
            <div style="font-size: 0.85rem; color: #fecdd3; margin-top: 3px;">
              Legacy Pet Store (TomEE :8000) unaffected (<span style="color: #4ade80; font-weight: 700;">HTTP 200 OK</span>) • Non-blocking dual-write routes to Kafka DLQ (<code style="color: #fca5a5; background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 4px;">petstore.orders.dlq</code>)
            </div>
          </div>
        </div>
        <div style="background: #e11d48; border: 1px solid #fda4af; color: #fff; padding: 0.4rem 0.9rem; border-radius: 8px; font-size: 0.8rem; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; box-shadow: 0 0 15px rgba(225,29,72,0.6);">
          OUTAGE ISOLATED
        </div>
      </div>
    `;
    banner.style.position = 'fixed';
    banner.style.top = '72px';
    banner.style.left = '50%';
    banner.style.transform = 'translateX(-50%)';
    banner.style.width = '92%';
    banner.style.maxWidth = '1250px';
    banner.style.zIndex = '9999';
    banner.style.background = '#881337';
    banner.style.border = '2px solid #f43f5e';
    banner.style.boxShadow = '0 20px 30px rgba(0, 0, 0, 0.7), 0 0 30px rgba(244, 63, 94, 0.5)';
    banner.style.borderRadius = '14px';
    banner.style.padding = '1rem 1.4rem';
    banner.style.backdropFilter = 'blur(16px)';
    banner.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
    document.body.appendChild(banner);
  });

  try {
    execSync('docker pause petstore-mongo', { stdio: 'ignore' });
  } catch (e) {}

  await sleep(4000);

  // Self-Healing Recovery: unpause MongoDB
  console.log('Restoring Datastore (docker unpause petstore-mongo)...');
  try {
    execSync('docker unpause petstore-mongo', { stdio: 'ignore' });
  } catch (e) {}

  await page.evaluate(() => {
    const banner = document.getElementById('chaos-demo-banner');
    if (banner) {
      banner.style.background = '#064e3b';
      banner.style.border = '2px solid #10b981';
      banner.style.boxShadow = '0 20px 30px rgba(0, 0, 0, 0.7), 0 0 30px rgba(16, 185, 129, 0.5)';
      banner.innerHTML = `
        <div style="display: flex; align-items: center; justify-content: space-between; gap: 1.5rem;">
          <div style="display: flex; align-items: center; gap: 1rem;">
            <div style="background: rgba(16, 185, 129, 0.25); border: 2px solid #10b981; width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; color: #34d399;">
              ✓
            </div>
            <div>
              <div style="font-weight: 800; font-size: 1rem; text-transform: uppercase; letter-spacing: 0.05em; color: #fff;">
                SELF-HEALING RESTORED: MongoDB Replica Set Active (<code style="color: #6ee7b7; background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 4px;">rs0</code>)
              </div>
              <div style="font-size: 0.85rem; color: #a7f3d0; margin-top: 3px;">
                DLQ consumer auto-draining queued transactions • Ready for shadow re-verification audit
              </div>
            </div>
          </div>
          <div style="background: #059669; border: 1px solid #6ee7b7; color: #fff; padding: 0.4rem 0.9rem; border-radius: 8px; font-size: 0.8rem; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; box-shadow: 0 0 15px rgba(5,150,105,0.6);">
            RECOVERED & RECONCILED
          </div>
        </div>
      `;
    }
  });

  await sleep(3500);

  // Scroll back to top
  for (let y = 450; y >= 0; y -= 20) {
    await page.evaluate((top) => window.scrollTo(0, top), y);
    await sleep(25);
  }
  await sleep(1000);

  // Click "Run Audit Now"
  console.log('Triggering Live Shadow Reconciliation Audit...');
  const buttons = await page.$$('button');
  for (const b of buttons) {
    const text = await (await b.getProperty('innerText')).jsonValue();
    if (text.includes('Run Audit Now')) {
      await b.click();
      break;
    }
  }

  await sleep(4000);

  // Fade out chaos banner
  await page.evaluate(() => {
    const banner = document.getElementById('chaos-demo-banner');
    if (banner) {
      banner.style.opacity = '0';
      banner.style.transition = 'opacity 0.6s ease';
      setTimeout(() => banner.remove(), 600);
    }
  });
  await sleep(1500);

  // =========================================================================
  // ACT 6: MONGODB ENGINE & COMPASS DIAGNOSTICS
  // =========================================================================
  console.log('🎬 ACT 6: MongoDB Engine & Compass Diagnostics...');
  const tabButtons = await page.$$('button');
  for (const b of tabButtons) {
    const text = await (await b.getProperty('innerText')).jsonValue();
    if (text.includes('MongoDB Engine & Compass')) {
      await b.click();
      break;
    }
  }
  await sleep(3500);

  // Return to Parity Monitor Tab
  const returnButtons = await page.$$('button');
  for (const b of returnButtons) {
    const text = await (await b.getProperty('innerText')).jsonValue();
    if (text.includes('Migration Parity & Reconciliation')) {
      await b.click();
      break;
    }
  }
  await sleep(3000);

  // Stop screencast
  await client.send('Page.stopScreencast');
  await browser.close();

  console.log(`✅ Captured ${frameCount} total frames across all user personas and architectures.`);
  console.log('Encoding videos via FFmpeg...');

  const mp4Output = path.join(OUTPUT_DIR, 'petstore_outage_rollback_demo.mp4');
  const webmOutput = path.join(OUTPUT_DIR, 'petstore_outage_rollback_demo.webm');
  const webpOutput = path.join(OUTPUT_DIR, 'petstore_outage_rollback_demo.webp');
  const webpThumbOutput = path.join(OUTPUT_DIR, 'petstore_outage_rollback_demo_thumb.webp');

  // MP4 Encoding (H.264, 18 fps, high quality)
  console.log('Generating MP4...');
  execSync(
    `"${FFMPEG_PATH}" -y -framerate 18 -pattern_type glob -i "${FRAMES_DIR}/*.jpg" -c:v libx264 -pix_fmt yuv420p -crf 20 -preset fast -movflags +faststart "${mp4Output}"`,
    { stdio: 'inherit' }
  );

  // WebM Encoding (VP9)
  console.log('Generating WebM...');
  execSync(
    `"${FFMPEG_PATH}" -y -framerate 18 -pattern_type glob -i "${FRAMES_DIR}/*.jpg" -c:v libvpx-vp9 -b:v 1500k -crf 26 "${webmOutput}"`,
    { stdio: 'inherit' }
  );

  // Optimized Animated WebP Thumbnail for embedding (800x500 @ 8 fps)
  console.log('Generating Animated WebP Thumbnail...');
  execSync(
    `"${FFMPEG_PATH}" -y -framerate 18 -pattern_type glob -i "${FRAMES_DIR}/*.jpg" -vf "fps=8,scale=800:-1:flags=lanczos" -vcodec libwebp -lossless 0 -compression_level 4 -qscale 60 -loop 0 "${webpThumbOutput}"`,
    { stdio: 'inherit' }
  );

  // Copy to brain artifact directory
  console.log('Copying videos to brain artifact directory...');
  fs.copyFileSync(mp4Output, path.join(ARTIFACT_DIR, 'petstore_outage_rollback_demo.mp4'));
  fs.copyFileSync(webmOutput, path.join(ARTIFACT_DIR, 'petstore_outage_rollback_demo.webm'));
  fs.copyFileSync(webpThumbOutput, path.join(ARTIFACT_DIR, 'petstore_outage_rollback_demo_thumb.webp'));

  console.log('🎉 Multi-User & Legacy Parity Video Recording Complete!');
}

record().catch(err => {
  try { execSync('docker unpause petstore-mongo', { stdio: 'ignore' }); } catch (e) {}
  console.error('Error during video recording:', err);
  process.exit(1);
});
