module.exports = {
  HOST: 'ep-falling-thunder-atnjous9-pooler.c-9.us-east-1.aws.neon.tech',
  USER: "neondb_owner",
  PASSWORD: "npg_aFuQlX12sMjh",
  DB: "neondb",
  dialect: "postgres",
  pool: {
    max: 5,
    min: 0,
    acquire: 30000,
    idle: 10000
  }
  };