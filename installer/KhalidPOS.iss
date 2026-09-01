; Khalid POS — Inno Setup 6 script
; Compile with build.bat at the repo root (not this file directly) so payload
; checks run first. Output: installer\output\KhalidPOS-Setup.exe

#define MyAppName        "Khalid POS"
#define MyAppVersion     "1.0.0"
#define MyAppPublisher   "Khalid Farms"
#define MyAppURL         "https://pos-api-production-91dc.up.railway.app"
#define MyServiceName    "KhalidPOS_MariaDB"
#define MyServiceTitle   "Khalid POS Database"
#define MyDbPort         "3307"

; Pre-filled on the till page. Paste the live Railway access key here so
; shopkeepers do not have to type it. Leave empty to let them paste it.
#define DefaultApiUrl    "https://pos-api-production-91dc.up.railway.app"
#define DefaultApiToken  ""

[Setup]
AppId={{E8B3C4A1-7D2F-4B91-9E6A-1F0C8A7B6D5E}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName=C:\KhalidPOS
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
; The till writes into the data folder — never Program Files.
PrivilegesRequired=admin
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
MinVersion=6.1sp1
OutputDir=output
OutputBaseFilename=KhalidPOS-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupLogging=yes
UninstallDisplayName={#MyAppName}
VersionInfoVersion={#MyAppVersion}
VersionInfoProductName={#MyAppName}
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription={#MyAppName} Setup
AllowNoIcons=no
UsePreviousAppDir=yes
DisableDirPage=no
CloseApplications=no
RestartIfNeededByRun=no
ChangesAssociations=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
SetupAppTitle=Khalid POS Setup
SetupWindowTitle=Khalid POS Setup
ClickNext=Click Next to continue.
FinishedHeadingLabel=Khalid POS is ready
ExitSetupTitle=Leave setup?
ExitSetupMessage=Setup is not complete. If you leave now, Khalid POS will not be installed.%n%nYou can run this installer again later.%n%nLeave setup?
ConfirmUninstall=Remove Khalid POS from this computer?%n%nYou will be asked whether to keep the shop records.

[Dirs]
Name: "{app}\tmp"
Name: "{app}\data"; Flags: uninsneveruninstall

[Files]
Source: "payload\jre\*"; DestDir: "{app}\jre"; Flags: recursesubdirs createallsubdirs ignoreversion
Source: "payload\mariadb\*"; DestDir: "{app}\mariadb"; Flags: recursesubdirs createallsubdirs ignoreversion
Source: "payload\app\*"; DestDir: "{app}\app"; Flags: recursesubdirs createallsubdirs ignoreversion
; Seed is copied only when this PC does not already have shop records.
Source: "payload\seed-data\*"; DestDir: "{app}\data"; Flags: recursesubdirs createallsubdirs uninsneveruninstall; Check: NeedSeedData
Source: "scripts\StartKhalidPOS.vbs"; DestDir: "{app}"; Flags: ignoreversion
Source: "scripts\StartKhalidPOS.cmd"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{sys}\wscript.exe"; Parameters: """{app}\StartKhalidPOS.vbs"""; WorkingDir: "{app}\app"; Comment: "Open Khalid POS"
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{sys}\wscript.exe"; Parameters: """{app}\StartKhalidPOS.vbs"""; WorkingDir: "{app}\app"; Comment: "Open Khalid POS"

[Run]
Filename: "{sys}\wscript.exe"; Parameters: """{app}\StartKhalidPOS.vbs"""; WorkingDir: "{app}\app"; Description: "Open Khalid POS now"; Flags: postinstall nowait skipifsilent; Check: SetupSucceeded

[UninstallDelete]
Type: filesandordirs; Name: "{app}\tmp"
Type: files; Name: "{app}\setup-status.txt"

[Code]
var
  TillPage: TWizardPage;
  TerminalEdit: TNewEdit;
  ApiUrlEdit: TNewEdit;
  ApiTokenEdit: TNewEdit;
  GSetupProblem: String;
  GSettingsNote: String;
  GDbReadable: Boolean;
  GSettingsSaved: Boolean;

function AppPath(const Suffix: String): String;
begin
  Result := ExpandConstant('{app}') + Suffix;
end;

function ToForwardSlashes(const S: String): String;
begin
  Result := S;
  StringChangeEx(Result, '\', '/', True);
end;

function SqlEscape(const S: String): String;
begin
  Result := S;
  StringChangeEx(Result, '\', '\\', True);
  StringChangeEx(Result, '''', '\''', True);
end;

function NeedSeedData: Boolean;
begin
  Result := not FileExists(AppPath('\data\ibdata1'));
end;

function SetupSucceeded: Boolean;
begin
  Result := (GSetupProblem = '');
end;

function RunHidden(const FileName, Params: String; var ResultCode: Integer): Boolean;
begin
  Result := Exec(FileName, Params, ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

function RunCmd(const Line: String; var ResultCode: Integer): Boolean;
begin
  Result := Exec(ExpandConstant('{cmd}'), '/S /C "' + Line + '"', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

function ServiceExists: Boolean;
var
  ResultCode: Integer;
begin
  Result := RunHidden(ExpandConstant('{sys}\sc.exe'), 'query {#MyServiceName}', ResultCode) and (ResultCode = 0);
end;

function IsPort3307Listening: Boolean;
var
  ResultCode: Integer;
begin
  Result := RunCmd('netstat -ano -p TCP | findstr /I /C:"LISTENING" | findstr /C:":{#MyDbPort}" >nul', ResultCode) and (ResultCode = 0);
end;

function PortBusyAndNotOurs: Boolean;
begin
  Result := IsPort3307Listening and (not ServiceExists);
end;

procedure StopAndRemoveService;
var
  ResultCode: Integer;
  I: Integer;
begin
  if not ServiceExists then
    Exit;
  RunHidden(ExpandConstant('{sys}\sc.exe'), 'stop {#MyServiceName}', ResultCode);
  I := 0;
  while (I < 20) and ServiceExists do
  begin
    Sleep(1000);
    RunHidden(ExpandConstant('{sys}\sc.exe'), 'query {#MyServiceName}', ResultCode);
    I := I + 1;
    if not IsPort3307Listening then
      Break;
  end;
  RunHidden(ExpandConstant('{sys}\sc.exe'), 'delete {#MyServiceName}', ResultCode);
  Sleep(1000);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  NeedsRestart := False;
  StopAndRemoveService;
  Result := '';
end;

function AddLabel(const Caption: String; Top, Height: Integer): TNewStaticText;
begin
  Result := TNewStaticText.Create(TillPage);
  Result.Parent := TillPage.Surface;
  Result.Caption := Caption;
  Result.Left := 0;
  Result.Top := ScaleY(Top);
  Result.Width := TillPage.SurfaceWidth;
  Result.Height := ScaleY(Height);
  Result.WordWrap := True;
  Result.AutoSize := False;
end;

function AddEdit(Top: Integer): TNewEdit;
begin
  Result := TNewEdit.Create(TillPage);
  Result.Parent := TillPage.Surface;
  Result.Left := 0;
  Result.Top := ScaleY(Top);
  Result.Width := TillPage.SurfaceWidth;
  Result.Height := ScaleY(22);
end;

procedure InitializeWizard;
begin
  WizardForm.WelcomeLabel2.Caption :=
    'This will install Khalid POS on this computer.'#13#10#13#10 +
    'You do not need to install anything else — everything the till needs is included.'#13#10#13#10 +
    'You will be asked which till this is.'#13#10#13#10 +
    'Click Next to continue.';

  WizardForm.SelectDirLabel.Caption :=
    'Khalid POS will be installed in this folder. The default is recommended because the till must be able to save shop records.';

  TillPage := CreateCustomPage(wpSelectDir,
    'Which till is this?',
    'Each checkout computer needs its own letter so sales from different tills stay distinct.');

  AddLabel('Till letter', 0, 16);
  TerminalEdit := AddEdit(18);
  TerminalEdit.MaxLength := 1;
  TerminalEdit.Text := GetPreviousData('TerminalId', 'B');
  AddLabel('One letter. Use A for the first till, B for the second, and so on.', 44, 28);

  AddLabel('Online address', 80, 16);
  ApiUrlEdit := AddEdit(98);
  ApiUrlEdit.Text := GetPreviousData('ApiUrl', '{#DefaultApiUrl}');
  AddLabel('Leave this unchanged unless you were given a different address.', 124, 28);

  AddLabel('Online access key', 160, 16);
  ApiTokenEdit := AddEdit(178);
  ApiTokenEdit.Text := GetPreviousData('ApiToken', '{#DefaultApiToken}');
  AddLabel('Leave this unchanged unless you were given a different key.', 204, 28);
end;

procedure RegisterPreviousData(PreviousDataKey: Integer);
begin
  SetPreviousData(PreviousDataKey, 'TerminalId', Trim(TerminalEdit.Text));
  SetPreviousData(PreviousDataKey, 'ApiUrl', Trim(ApiUrlEdit.Text));
  SetPreviousData(PreviousDataKey, 'ApiToken', Trim(ApiTokenEdit.Text));
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  Tid: String;
begin
  Result := True;

  if CurPageID = TillPage.ID then
  begin
    Tid := UpperCase(Trim(TerminalEdit.Text));
    if Tid = '' then
      Tid := 'B';
    if (Length(Tid) <> 1) or (Tid[1] < 'A') or (Tid[1] > 'Z') then
    begin
      MsgBox('Please enter one letter for this till, for example B.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    TerminalEdit.Text := Tid;

    if Trim(ApiUrlEdit.Text) = '' then
    begin
      MsgBox('Please fill in the online address, or put back the default value.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;

  if CurPageID = wpReady then
  begin
    if PortBusyAndNotOurs then
    begin
      if MsgBox(
           'Another program is already using the shop database connection (port {#MyDbPort}).'#13#10#13#10 +
           'Close that program first, then try again.'#13#10#13#10 +
           'Do you want to stop the installation?',
           mbConfirmation, MB_YESNO or MB_DEFBUTTON1) = IDYES then
        Result := False;
    end;
  end;
end;

procedure WriteMyIni;
var
  Ini, S: String;
begin
  Ini := AppPath('\my.ini');
  S :=
    '[client]' + #13#10 +
    'port={#MyDbPort}' + #13#10 +
    'host=127.0.0.1' + #13#10 +
    #13#10 +
    '[mysqld]' + #13#10 +
    'port={#MyDbPort}' + #13#10 +
    'bind-address=127.0.0.1' + #13#10 +
    'basedir="' + ToForwardSlashes(AppPath('\mariadb')) + '"' + #13#10 +
    'datadir="' + ToForwardSlashes(AppPath('\data')) + '"' + #13#10 +
    'tmpdir="' + ToForwardSlashes(AppPath('\tmp')) + '"' + #13#10;
  if DirExists(AppPath('\mariadb\lib\plugin')) then
    S := S + 'plugin_dir="' + ToForwardSlashes(AppPath('\mariadb\lib\plugin')) + '"' + #13#10;
  S := S +
    'character-set-server=utf8mb4' + #13#10 +
    'collation-server=utf8mb4_general_ci' + #13#10 +
    'max_connections=50' + #13#10 +
    'innodb_buffer_pool_size=128M' + #13#10 +
    'skip-name-resolve' + #13#10 +
    'default_storage_engine=InnoDB' + #13#10;
  SaveStringToFile(Ini, S, False);
end;

function Mysqladmin(const Args: String; var ResultCode: Integer): Boolean;
begin
  Result := RunHidden(AppPath('\mariadb\bin\mysqladmin.exe'), Args, ResultCode);
end;

function WaitForDatabase(TimeoutSec: Integer): Boolean;
var
  I, ResultCode: Integer;
  Args: String;
begin
  Args := '--protocol=TCP --host=127.0.0.1 --port={#MyDbPort} --user=root ping';
  I := 0;
  while I < TimeoutSec do
  begin
    if Mysqladmin(Args, ResultCode) and (ResultCode = 0) then
    begin
      Result := True;
      Exit;
    end;
    Sleep(1000);
    I := I + 1;
  end;
  Result := False;
end;

function StartMysqldStandalone: Boolean;
var
  ResultCode: Integer;
  Params: String;
begin
  Params := '--defaults-file="' + AppPath('\my.ini') + '"';
  Result := Exec(AppPath('\mariadb\bin\mysqld.exe'), Params, AppPath('\mariadb'), SW_HIDE, ewNoWait, ResultCode);
end;

procedure ShutdownMysqld;
var
  ResultCode: Integer;
begin
  Mysqladmin('--protocol=TCP --host=127.0.0.1 --port={#MyDbPort} --user=root shutdown', ResultCode);
end;

procedure WaitUntilPortFree(TimeoutSec: Integer);
var
  I: Integer;
begin
  I := 0;
  while (I < TimeoutSec) and IsPort3307Listening do
  begin
    Sleep(1000);
    I := I + 1;
  end;
end;

function ApplyTillSettings: Boolean;
var
  PengaturanSql, SyncSql, BatFile, Mysql, Token, SyncFlag: String;
  ResultCode: Integer;
begin
  Token := Trim(ApiTokenEdit.Text);
  if Token = '' then
    SyncFlag := '0'
  else
    SyncFlag := '1';

  Mysql := '"' + AppPath('\mariadb\bin\mysql.exe') + '" --protocol=TCP --host=127.0.0.1 --port={#MyDbPort} --user=root --default-character-set=utf8mb4 pointofsale';

  PengaturanSql :=
    'INSERT INTO pengaturan (setting_key, setting_value) VALUES ' +
    '(''terminal_id'', ''' + SqlEscape(Trim(TerminalEdit.Text)) + '''), ' +
    '(''api_base_url'', ''' + SqlEscape(Trim(ApiUrlEdit.Text)) + '''), ' +
    '(''api_token'', ''' + SqlEscape(Token) + '''), ' +
    '(''sync_enabled'', ''' + SyncFlag + ''') ' +
    'ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);' + #13#10;

  SyncSql :=
    'INSERT INTO sync_state (state_key, state_value) VALUES ' +
    '(''api_base_url'', ''' + SqlEscape(Trim(ApiUrlEdit.Text)) + '''), ' +
    '(''api_token'', ''' + SqlEscape(Token) + ''') ' +
    'ON DUPLICATE KEY UPDATE state_value = VALUES(state_value);' + #13#10;

  SaveStringToFile(ExpandConstant('{tmp}\till-pengaturan.sql'), PengaturanSql, False);
  SaveStringToFile(ExpandConstant('{tmp}\till-sync.sql'), SyncSql, False);

  BatFile := ExpandConstant('{tmp}\apply-till-settings.bat');
  SaveStringToFile(BatFile,
    '@echo off' + #13#10 +
    Mysql + ' < "' + ExpandConstant('{tmp}\till-pengaturan.sql') + '"' + #13#10 +
    'set "PERR=%ERRORLEVEL%"' + #13#10 +
    Mysql + ' < "' + ExpandConstant('{tmp}\till-sync.sql') + '"' + #13#10 +
    'exit /b %PERR%' + #13#10,
    False);

  Result := RunCmd('"' + BatFile + '"', ResultCode) and (ResultCode = 0);
end;

function VerifyProdukReadable: Boolean;
var
  BatFile: String;
  ResultCode: Integer;
begin
  BatFile := ExpandConstant('{tmp}\verify-produk.bat');
  SaveStringToFile(BatFile,
    '@echo off' + #13#10 +
    '"' + AppPath('\mariadb\bin\mysql.exe') + '" ' +
    '--protocol=TCP --host=127.0.0.1 --port={#MyDbPort} --user=root ' +
    '--default-character-set=utf8mb4 --batch --silent ' +
    '-e "SELECT 1 FROM pointofsale.produk LIMIT 1;"' + #13#10,
    False);
  Result := RunCmd('"' + BatFile + '"', ResultCode) and (ResultCode = 0);
end;

function InstallAndStartService: Boolean;
var
  ResultCode: Integer;
  Defaults: String;
begin
  Result := False;
  Defaults := '--install {#MyServiceName} --defaults-file="' + AppPath('\my.ini') + '"';
  if not RunHidden(AppPath('\mariadb\bin\mysqld.exe'), Defaults, ResultCode) then
    Exit;
  if ResultCode <> 0 then
    Exit;

  RunHidden(ExpandConstant('{sys}\sc.exe'), 'config {#MyServiceName} start= auto', ResultCode);
  RunHidden(ExpandConstant('{sys}\sc.exe'), 'config {#MyServiceName} DisplayName= "{#MyServiceTitle}"', ResultCode);
  RunHidden(ExpandConstant('{sys}\sc.exe'),
    'description {#MyServiceName} "Shop records for Khalid POS. Do not stop this while the till is in use."',
    ResultCode);
  // Allow signed-in users to start the service without an administrator prompt.
  RunHidden(ExpandConstant('{sys}\sc.exe'),
    'sdset {#MyServiceName} D:(A;;CCLCSWRPWPDTLOCRRC;;;SY)(A;;CCDCLCSWRPWPDTLOCRSDRCWDWO;;;BA)(A;;CCLCSWLOCRRC;;;IU)(A;;CCLCSWLOCRRC;;;SU)(A;;RPWPLCLOCR;;;AU)',
    ResultCode);

  RunHidden(ExpandConstant('{sys}\sc.exe'), 'start {#MyServiceName}', ResultCode);
  Result := True;
end;

procedure WriteStatusFile;
var
  S, Readable, Saved: String;
begin
  if GDbReadable then Readable := 'yes' else Readable := 'no';
  if GSettingsSaved then Saved := 'yes' else Saved := 'no';
  S :=
    'Khalid POS setup notes' + #13#10 +
    '======================' + #13#10 +
    'Till letter: ' + Trim(TerminalEdit.Text) + #13#10 +
    'Product list readable: ' + Readable + #13#10 +
    'Till settings saved: ' + Saved + #13#10;
  if GSetupProblem <> '' then
    S := S + 'Result: could not finish' + #13#10
  else
    S := S + 'Result: ready' + #13#10;
  SaveStringToFile(AppPath('\setup-status.txt'), S, False);
end;

procedure FinishDatabaseSetup;
var
  StandaloneStarted, StandaloneOk: Boolean;
begin
  GSetupProblem := '';
  GSettingsNote := '';
  GDbReadable := False;
  GSettingsSaved := False;

  WriteMyIni;
  ForceDirectories(AppPath('\tmp'));

  StandaloneStarted := StartMysqldStandalone;
  StandaloneOk := StandaloneStarted and WaitForDatabase(45);
  if StandaloneOk then
  begin
    GSettingsSaved := ApplyTillSettings;
    GDbReadable := VerifyProdukReadable;
  end;
  if StandaloneStarted then
  begin
    ShutdownMysqld;
    WaitUntilPortFree(20);
  end;

  if not InstallAndStartService then
  begin
    GSetupProblem :=
      'The shop records could not be started on this computer.'#13#10#13#10 +
      'Please restart the computer and open Khalid POS from the Desktop shortcut.'#13#10#13#10 +
      'If it still does not open, run this installer again. If that does not help, contact support.';
    WriteStatusFile;
    MsgBox(GSetupProblem, mbError, MB_OK);
    Exit;
  end;

  if not WaitForDatabase(30) then
  begin
    GSetupProblem :=
      'The shop records could not be started on this computer.'#13#10#13#10 +
      'Please restart the computer and open Khalid POS from the Desktop shortcut.'#13#10#13#10 +
      'If it still does not open, run this installer again. If that does not help, contact support.';
    WriteStatusFile;
    MsgBox(GSetupProblem, mbError, MB_OK);
    Exit;
  end;

  if not GSettingsSaved then
    GSettingsSaved := ApplyTillSettings;
  if not GDbReadable then
    GDbReadable := VerifyProdukReadable;

  if not GDbReadable then
  begin
    GSetupProblem :=
      'Khalid POS was copied to this computer, but the product list could not be read.'#13#10#13#10 +
      'Please restart the computer and try again. If it still fails, contact support and do not delete the KhalidPOS folder — your shop records may still be there.';
    WriteStatusFile;
    MsgBox(GSetupProblem, mbError, MB_OK);
    Exit;
  end;

  if not GSettingsSaved then
    GSettingsNote :=
      #13#10#13#10 +
      'This till''s letter could not be saved automatically. You can still open Khalid POS.';

  WriteStatusFile;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
    FinishDatabaseSetup;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = wpFinished then
  begin
    if GSetupProblem <> '' then
    begin
      WizardForm.FinishedHeadingLabel.Caption := 'Setup could not finish';
      WizardForm.FinishedLabel.Caption := GSetupProblem;
      WizardForm.RunList.Visible := False;
    end
    else
    begin
      WizardForm.FinishedHeadingLabel.Caption := 'Khalid POS is ready';
      WizardForm.FinishedLabel.Caption :=
        'Khalid POS is installed. Open it from the Desktop or the Start menu.'#13#10#13#10 +
        'Use the same sign-in you already use in the shop.' +
        GSettingsNote;
    end;
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
    StopAndRemoveService;

  if CurUninstallStep = usPostUninstall then
  begin
    { Default button is Yes — keep shop records. Never delete sales silently. }
    if MsgBox(
         'Keep the shop records on this computer?'#13#10#13#10 +
         'Sales, products, and customers will be kept if you choose Yes. This is recommended.'#13#10#13#10 +
         'Choose No only if you want to wipe this till completely. This cannot be undone.',
         mbConfirmation, MB_YESNO or MB_DEFBUTTON1) = IDNO then
    begin
      DelTree(ExpandConstant('{app}'), True, True, True);
    end
    // Choosing Yes leaves data\ and my.ini so a later reinstall can reuse this till.
  end;
end;
