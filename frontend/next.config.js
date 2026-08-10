/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  // https://github.com/twbs/bootstrap/issues/40962#issuecomment-3436817277
  sassOptions: {
    silenceDeprecations: [
      "mixed-decls",
      "color-functions",
      "global-builtin",
      "import",
      "if-function",
    ],
  },
};

module.exports = nextConfig;
