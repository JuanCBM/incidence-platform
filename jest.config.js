export default {
  testEnvironment: "node",
  transform: {},
  testMatch: ["<rootDir>/tests/**/*.test.js"],
  collectCoverageFrom: [
    "mcp/**/*.js",
    "!node_modules/**",
  ],
  coveragePathIgnorePatterns: ["/node_modules/"],
};
