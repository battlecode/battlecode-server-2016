package battlecode.instrumenter;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.server.Config;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SandboxedRobotPlayer; i.e.
 *
 * @author james
 */
public class SandboxedRobotPlayerTest {

    IndividualClassLoader.Cache cache;
    RobotController rc;

    @Before
    public void setupController() {
        // Uses the "mockito" library to create a mock RobotController object,
        // so that we don't have to create a GameWorld and all that
        rc = mock(RobotController.class);

        // SandboxedRobotPlayer uses rc.getTeam; tell it we're team A
        when(rc.getTeam()).thenReturn(Team.A);
        when(rc.getType()).thenReturn(RobotType.ARCHON);
        when(rc.getID()).thenReturn(0);
        when(rc.getLocation()).thenReturn(new MapLocation(0, 0));
        when(rc.getRoundNum()).thenReturn(0);

        cache = new IndividualClassLoader.Cache();
    }

    @Test
    public void testLifecycleEmptyPlayer() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayerempty", rc, 0, cache);

        player.setBytecodeLimit(10000);

        player.step();

        // Player should immediately return.

        assertTrue(player.getTerminated());
    }

    @Test
    public void testRobotControllerMethodsCalled() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayeractions", rc, 0, cache);

        player.setBytecodeLimit(10000);

        player.step();

        assertTrue(player.getTerminated());

        // Make sure that the player called the correct methods

        verify(rc).addMatchObservation("text");
        verify(rc).readSignal();
        verify(rc).resign();
        verify(rc).senseNearbyRobots();
        verify(rc).setTeamMemory(0, 0);
    }

    @Test
    public void testYield() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayerclock", rc, 0, cache);
        player.setBytecodeLimit(10000);

        player.step();

        assertFalse(player.getTerminated());
        verify(rc).broadcastSignal(1);

        player.step();

        assertFalse(player.getTerminated());
        verify(rc).broadcastSignal(2);

        player.step();

        assertTrue(player.getTerminated());
        verify(rc).broadcastSignal(3);
    }

    @Test
    public void testBytecodeCountingWorks() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayerloopforever", rc, 0, cache);
        player.setBytecodeLimit(100);

        player.step();

        // The real test is whether step returns at all, since the player doesn't yield or terminate;
        // still, we should test that the player hasn't terminated

        assertFalse(player.getTerminated());

    }

    @Test
    public void testBytecodeCountsCorrect() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayerclock", rc, 0, cache);
        player.setBytecodeLimit(10000);

        player.step();

        verify(rc).broadcastSignal(1);
        // broadcast() is 100 bytecodes, +2 extra
        assertEquals(player.getBytecodesUsed(), 102);
    }

    @Test(timeout=300)
    public void testAvoidDeadlocks() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayersuicide", rc, 0, cache);
        player.setBytecodeLimit(10);

        // Attempt to kill the player when it calls "disintegrate"
        // This used to deadlock because both step() and terminate() were synchronized.
        doAnswer(invocation -> {
            player.terminate();
            return null;
        }).when(rc).disintegrate();

        player.step();

        // And if the method returns, we know we have no deadlocks.
        assertTrue(player.getTerminated());
    }

    @Test
    public void testStaticInitialization() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayerstatic", rc, 0, cache);
        player.setBytecodeLimit(10000);

        // Player calls "yield" in static initializer
        player.step();
        assertFalse(player.getTerminated());

        // Player terminates when actual "run" starts
        player.step();
        assertTrue(player.getTerminated());
    }

    @Test
    public void testBytecodeOveruse() throws Exception {
        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayerbytecode", rc, 0, cache);
        player.setBytecodeLimit(200);

        for (int i = 0; i < 5; i++) {
            player.step();
            assertFalse(player.getTerminated());
        }

        player.step();
        assertTrue(player.getTerminated());
    }

    @Test
    public void testBcTesting() throws Exception {
        Config.getGlobalConfig().set("bc.testing.should.terminate", "true");

        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayersystem", rc, 0, cache);
        player.setBytecodeLimit(200);

        player.step();
        assertTrue(player.getTerminated());
    }

    @Test
    public void testDebugMethodsEnabled() throws Exception {
        Config.getGlobalConfig().set("bc.engine.debug-methods", "true");

        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayerdebug", rc, 0, cache);
        player.setBytecodeLimit(100);

        player.step();
        assertTrue(player.getTerminated());
    }

    @Test
    public void testDebugMethodsDisabled() throws Exception {
        Config.getGlobalConfig().set("bc.engine.debug-methods", "false");

        SandboxedRobotPlayer player = new SandboxedRobotPlayer("testplayernodebug", rc, 0, cache);
        player.setBytecodeLimit(200);

        player.step();
        assertTrue(player.getTerminated());
    }
}
