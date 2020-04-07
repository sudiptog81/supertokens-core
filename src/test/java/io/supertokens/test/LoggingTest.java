/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This program is licensed under the SuperTokens Community License (the
 *    "License") as published by VRAI Labs. You may not use this file except in
 *    compliance with the License. You are not permitted to transfer or
 *    redistribute this file without express written permission from VRAI Labs.
 *
 *    A copy of the License is available in the file titled
 *    "SuperTokensLicense.pdf" inside this repository or included with your copy of
 *    the software or its source code. If you have not received a copy of the
 *    License, please write to VRAI Labs at team@supertokens.io.
 *
 *    Please read the License carefully before accessing, downloading, copying,
 *    using, modifying, merging, transferring or sharing this software. By
 *    undertaking any of these activities, you indicate your agreement to the terms
 *    of the License.
 *
 *    This program is distributed with certain software that is licensed under
 *    separate terms, as designated in a particular file or component or in
 *    included license documentation. VRAI Labs hereby grants you an additional
 *    permission to link the program and your derivative works with the separately
 *    licensed software that they have included with this program, however if you
 *    modify this program, you shall be solely liable to ensure compliance of the
 *    modified program with the terms of licensing of the separately licensed
 *    software.
 *
 *    Unless required by applicable law or agreed to in writing, this program is
 *    distributed under the License on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *
 */

package io.supertokens.test;

import ch.qos.logback.classic.Logger;
import io.supertokens.ProcessState.EventAndException;
import io.supertokens.ProcessState.PROCESS_STATE;
import io.supertokens.cliOptions.CLIOptions;
import io.supertokens.config.Config;
import io.supertokens.output.Logging;
import io.supertokens.test.TestingProcessManager.TestingProcess;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Scanner;

import static org.junit.Assert.*;

public class LoggingTest {
    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    @Test
    public void defaultLogging() throws Exception {
        String[] args = {"../", "DEV"};
        TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

        Logging.error(process.getProcess(), "From test", false);

        boolean infoFlag = false;
        boolean errorFlag = false;

        File infoLog = new File(Config.getConfig(process.getProcess()).getInfoLogPath(process.getProcess()));
        File errorLog = new File(Config.getConfig(process.getProcess()).getErrorLogPath(process.getProcess()));

        try (Scanner scanner = new Scanner(infoLog)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(process.getProcess().getProcessId())) {
                    infoFlag = true;
                    break;
                }
            }
        }

        try (Scanner errorScanner = new Scanner(errorLog)) {
            while (errorScanner.hasNextLine()) {
                String line = errorScanner.nextLine();
                if (line.contains(process.getProcess().getProcessId())) {
                    errorFlag = true;
                    break;
                }
            }
        }

        assertTrue(infoFlag && errorFlag);

        process.kill();
        EventAndException event1 = process.checkOrWaitForEvent(PROCESS_STATE.STOPPED);
        assertNotNull(event1);

    }

    @Test
    public void customLogging() throws Exception {
        try {
            String[] args = {"../", "DEV"};

            Utils.setValueInConfig("info_log_path", "\"tempLogging/info.log\"");
            Utils.setValueInConfig("error_log_path", "\"tempLogging/error.log\"");

            TestingProcess process = TestingProcessManager.start(args);
            assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

            Logging.error(process.getProcess(), "From Test", false);
            Logging.info(process.getProcess(), "From Test");

            boolean infoFlag = false;
            boolean errorFlag = false;

            File infoLog = new File(Config.getConfig(process.getProcess()).getInfoLogPath(process.getProcess()));
            File errorLog = new File(Config.getConfig(process.getProcess()).getErrorLogPath(process.getProcess()));

            try (Scanner scanner = new Scanner(infoLog)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains(process.getProcess().getProcessId())) {
                        infoFlag = true;
                        break;
                    }
                }
            }

            try (Scanner errorScanner = new Scanner(errorLog)) {
                while (errorScanner.hasNextLine()) {
                    String line = errorScanner.nextLine();
                    if (line.contains(process.getProcess().getProcessId())) {
                        errorFlag = true;
                        break;
                    }
                }
            }

            assertTrue(infoFlag && errorFlag);
            process.kill();
            EventAndException event1 = process.checkOrWaitForEvent(PROCESS_STATE.STOPPED);
            assertNotNull(event1);

        } finally {

            FileUtils.deleteDirectory(new File("tempLogging"));

        }
    }

    @Test
    public void confirmLoggerClosed() throws Exception {

        String[] args = {"../", "DEV"};
        TestingProcess process = TestingProcessManager.start(args);

        assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

        Logger comInfoLog = (Logger) LoggerFactory
                .getLogger("io.supertokens.Info." + process.getProcess().getProcessId());
        Logger comErrorLog = (Logger) LoggerFactory
                .getLogger("io.supertokens.Error." + process.getProcess().getProcessId());


        java.util.logging.Logger webLogger = java.util.logging.Logger.getLogger("org.apache");

        assertTrue(comInfoLog.iteratorForAppenders().hasNext() && comErrorLog.iteratorForAppenders().hasNext());
        assertEquals(1, webLogger.getHandlers().length);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));

        assertFalse(comInfoLog.iteratorForAppenders().hasNext() || comErrorLog.iteratorForAppenders().hasNext());
        assertEquals(0, webLogger.getHandlers().length);
    }

    @Test
    public void testStandardOutLoggingWithNullStr() throws Exception {
        String[] args = {"../", "DEV"};
        ByteArrayOutputStream stdOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();

        Utils.setValueInConfig("info_log_path", "\"null\"");
        Utils.setValueInConfig("error_log_path", "\"null\"");


        System.setOut(new PrintStream(stdOutput));
        System.setErr(new PrintStream(errorOutput));

        TestingProcess process = TestingProcessManager.start(args, false);


        try {
            process.startProcess();
            assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

            Logging.debug(process.getProcess(), "outTest-dfkn3knsakn");
            Logging.error(process.getProcess(), "errTest-sdvjovnoasid", false);

            assertTrue(fileContainsString(stdOutput, "outTest-dfkn3knsakn"));
            assertTrue(fileContainsString(stdOutput, "errTest-sdvjovnoasid"));


        } finally {
            process.kill();
            assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        }

    }

    @Test
    public void testStandardOutLoggingWithNull() throws Exception {
        String[] args = {"../", "DEV"};
        ByteArrayOutputStream stdOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();

        Utils.setValueInConfig("info_log_path", "null");
        Utils.setValueInConfig("error_log_path", "null");


        System.setOut(new PrintStream(stdOutput));
        System.setErr(new PrintStream(errorOutput));

        TestingProcess process = TestingProcessManager.start(args, false);


        try {
            process.startProcess();
            assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

            Logging.debug(process.getProcess(), "outTest-dfkn3knsakn");
            Logging.error(process.getProcess(), "errTest-sdvjovnoasid", false);

            assertTrue(fileContainsString(stdOutput, "outTest-dfkn3knsakn"));
            assertTrue(fileContainsString(stdOutput, "errTest-sdvjovnoasid"));


        } finally {
            process.kill();
            assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        }

    }

    @Test
    public void testThatSubFoldersAreCreated() throws Exception {
        String[] args = {"../", "DEV"};


        TestingProcess process = TestingProcessManager.start(args, false);
        try {
            Utils.setValueInConfig("info_log_path", "../temp/a/b/info.log");
            process.startProcess();
            assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

            File logFile = new File("../temp/a");
            assertTrue(logFile.isDirectory());

            logFile = new File("../temp/a/b");
            assertTrue(logFile.isDirectory());


            logFile = new File("../temp/a/b/info.log");
            assertFalse(logFile.isDirectory());

        } finally {

            FileUtils.deleteDirectory(new File("../temp/a"));

            process.kill();
            assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
        }

    }


    @Test
    public void testDefaultLoggingFilePath() throws Exception {
        String[] args = {"../", "DEV"};
        TestingProcess process = TestingProcessManager.start(args);

        assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

        assertEquals(Config.getConfig(process.getProcess()).getInfoLogPath(process.getProcess()),
                CLIOptions.get(process.getProcess()).getInstallationPath() + "logs/info.log");

        assertEquals(Config.getConfig(process.getProcess()).getErrorLogPath(process.getProcess()),
                CLIOptions.get(process.getProcess()).getInstallationPath() + "logs/error.log");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));

    }

    private static boolean fileContainsString(ByteArrayOutputStream log, String value)
            throws IOException {
        boolean containsString = false;
        try (BufferedReader reader = new BufferedReader(new StringReader(log.toString()))) {
            String currentReadingLine = reader.readLine();
            while (currentReadingLine != null) {
                if (currentReadingLine.contains(value)) {
                    containsString = true;
                    break;
                }
                currentReadingLine = reader.readLine();
            }
        }
        return containsString;
    }

}
