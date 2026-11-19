# Test Execution Report — OpenCart User Registration

- **Date:** 19/11/2026 08:18:18 – 08:26:02
- **Browser:** Chrome
- **Base URL:** `http://localhost/opencart-4.1.0.4/upload/`
- **Source Log:** `src\main\resources\logs\TestLog_20261119_081818.txt`

## Automation Test Results

| Test Case                                                            | Test Input / Target | Expected Behavior | Automation Result | Details / Notes |
|:---------------------------------------------------------------------| :--- | :--- | :---: | :--- |
| **TC_121**                                                           | `FirstName='María'`, `LastName='García'`, `Email='maria.garcia.20250101@example.com'`, `Password='P@ssw0rd!'` | `SUCCESS` | **PASS** | Registered successfully (diacritics & special password chars). |
| **TC_089**                                                           | `FirstName='A'` (1 char), `LastName='Doe'`, `Email='firstname_1char@example.com'` | `SUCCESS` | **PASS** | Registered successfully (min first name length = 1). |
| **TC_089**                                                           | `FirstName='A' * 32` (32 chars), `LastName='Doe'`, `Email='firstname_32chars@example.com'` | `SUCCESS` | **PASS** | Registered successfully (max first name length = 32). |
| **TC_089**                                                           | `FirstName='A' * 33` (33 chars), `LastName='Doe'`, `Email='firstname_33chars@example.com'` | `FAILURE` | **PASS** | Expected failure reproduced: *"First Name must be between 1 and 32 characters!"* |
| **TC_089**                                                           | `FirstName='John'`, `LastName='A'` (1 char), `Email='lastname_1char@example.com'` | `SUCCESS` | **PASS** | Registered successfully (min last name length = 1). |
| **TC_089**                                                           | `FirstName='John'`, `LastName='A' * 32` (32 chars), `Email='lastname_32chars@example.com'` | `SUCCESS` | **PASS** | Registered successfully (max last name length = 32). |
| **TC_089**                                                           | `FirstName='John'`, `LastName='A' * 33` (33 chars), `Email='lastname_33chars@example.com'` | `FAILURE` | **PASS** | Expected failure reproduced: *"Last Name must be between 1 and 32 characters!"* |
| **TC_090**                                                           | `Email='pwd_3chars@example.com'`, `Password='123'` (3 chars) | `FAILURE` | **PASS** | Expected failure reproduced: *"Password must be between 6 and 40 characters!"* |
| **Data-Driven Case 9/18**                                            | `Email='pwd_6chars@example.com'`, `Password='123456'` (6 chars) | `SUCCESS` | **PASS** | Registered successfully (min password length = 6). |
| **TC_090**                                                           | `Email='pwd_20chars@example.com'`, `Password='P@ssw0rdP@ssw0rd1234'` (20 chars) | `SUCCESS` | **PASS** | Registered successfully. |
| **Data-Driven Case 11/18**                                           | `Email='pwd_40chars@example.com'`, `Password` (40 chars) | `SUCCESS` | **PASS** | Registered successfully (max password length = 40). |
| **Data-Driven Case 12/18**                                           | `Email='pwd_41chars@example.com'`, `Password` (41 chars) | `FAILURE` | **PASS** | Expected failure reproduced: *"Password must be between 6 and 40 characters!"* |
| **TC_088**                                                           | `Email='plainaddress'` | `FAILURE` | **PASS** | Expected failure reproduced: *"E-Mail Address does not appear to be valid!"* |
| **TC_088**                                                           | `Email='@missingusername.com'` | `FAILURE` | **PASS** | Expected failure reproduced: *"E-Mail Address does not appear to be valid!"* |
| **TC_088**                                                           | `Email='user@.com'` | `FAILURE` | **PASS** | Expected failure reproduced: *"E-Mail Address does not appear to be valid!"* |
| **TC_088**                                                           | `Email='user@domain..com'` | `FAILURE` | **PASS** | Expected failure reproduced: *"E-Mail Address does not appear to be valid!"* |
| **TC_088**                                                           | `Email='user space@domain.com'` | `FAILURE` | **PASS** | Expected failure reproduced: *"E-Mail Address does not appear to be valid!"* |
| **TC_088**                                                           | `Email='name@domain'` | `FAILURE` | **PASS** | Expected failure reproduced: *"E-Mail Address does not appear to be valid!"* |
| **TC_087**                                                           | All required fields empty (`Email: <empty>`) | All 4 field-level validation errors shown | **PASS** | All four field errors shown as expected; all-required-empty case reproduced correctly. |
| **TC_091**                                                           | Privacy Policy NOT accepted (`noprivacy_1795069361151@example.com`) | Blocked with warning & no account created | **PASS** | Blocked with *"Warning: You must agree to the Privacy Policy!"*; login refused (no account persisted). |
| **TC_092**                                                           | Privacy toggle recovery (`recovery_1795069374780@example.com`) | Warn without privacy, succeed after enabling | **PASS** | Warning shown on first submit; registration succeeded after accepting Privacy Policy. |
| **TC_093**                                                           | Newsletter ON (`nl_on_1795069379198@example.com`) | Registration succeeds & newsletter state checked | **PASS** | Registration succeeded; navigated to login/newsletter check (`selected: false` logged). |
| **TC_094**                                                           | Newsletter default OFF (`nl_off_1795069394695@example.com`) | Default OFF & subscription remains unchecked | **PASS** | Default toggle OFF; newsletter subscription verified OFF (checkbox unchecked). |
| **TC_125**                                                           | Duplicate email prevention (`dup_1795069426456@example.com`) | 1st succeeds, 2nd fails with duplicate warning | **PASS** | Duplicate registration prevented: *"Warning: E-Mail Address is already registered!"* |
| **TC_127**                                                           | Leading/trailing spaces (`  spaces_1795069446147@example.com  `) | Trimmed consistently & authenticates | **PASS** | Padded input accepted (trimmed); login with trimmed email succeeded. |
| **TC_128**                                                           | Email case consistency (`case_1795069464856@example.com` vs uppercase) | Case-insensitive duplicate rejected | **PASS** | Lowercase succeeded; uppercase variant rejected with *"Warning: E-Mail Address is already registered!"* |
| **TC_129**                                                           | Markup safe rendering (`<b>QA_CANARY</b>`, `markup_1795069484291@example.com`) | Encoded safely without HTML execution | **PASS** | Markup encoded as text; no unsafe HTML rendering. |
| **TC_130**                                                           | SQL metacharacters safety (`O'Connor` & SQL payload) | Accepted safely with no SQL/PHP/mysqli errors | **PASS** | Apostrophe and SQL-like payload handled as plain text with no errors or crash. |
| **TC_131**                                                           | Password not exposed in URL/errors (`leak_1795069550703@example.com`) | Password masked (`type="password"`) & not leaked | **PASS** | Password not exposed in URL, title, or error text; input type verified as `'password'`. |
| **TC_132** (Includes **TC_122**, **TC_123**, **TC_124**, **TC_126**) | Field-error association (`fe_*`) | Each error maps strictly to its target field | **PASS** | Verified for `firstname`, `lastname`, `email`, and `password`—only target field had an error. |