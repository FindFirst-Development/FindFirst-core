import {
  DockerComposeEnvironment,
  StartedDockerComposeEnvironment,
  Wait,
} from "testcontainers";
import path from "node:path";

const __root_dirname = process.cwd();
const composeFilePath = path.resolve(__root_dirname, "../");
const composeFile = "docker-compose.yml";

// export let composeEnv: StartedDockerComposeEnvironment
export let composeEnv: StartedDockerComposeEnvironment;

export default async function setup() {
  console.log("spinning up docker");
  console.log(composeFilePath);
  composeEnv = await new DockerComposeEnvironment(composeFilePath, composeFile)
    .withWaitStrategy(
      "server-1",
      Wait.forLogMessage("Started FindFirstApplication"),
    )
    .withWaitStrategy("frontend-1", Wait.forLogMessage("Ready in"))
    .withWaitStrategy("screenshot-1", Wait.forListeningPorts())
    // .withStartupTimeout(120_000) // Extend timeout to 120 seconds
    .up(["db", "frontend", "server"]);
}
