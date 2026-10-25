module.exports = {
  HOST: 'aws-0-us-east-1.pooler.supabase.com',
  USER: 'postgres.runmfjrhshpxzroyqohi',
  PASSWORD: 'P5toepGepQZg36Vm', 
  DB: 'postgres',
  PORT: 6543,
  dialect: 'postgres',
  dialectOptions: {
    ssl: {
      require: true,
      rejectUnauthorized: false
    }
  },
  pool: {
    max: 5,
    min: 0,
    acquire: 30000,
    idle: 10000
  }
};