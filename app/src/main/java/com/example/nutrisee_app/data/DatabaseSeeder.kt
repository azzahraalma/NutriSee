package com.example.nutrisee.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseSeeder {

    fun seed(db: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val userDao    = db.userDao()
            val profileDao = db.userProfileDao()

            if (userDao.getUserByEmail("test@gmail.com") == null) {
                userDao.register(
                    User(namaLengkap = "User Test", email = "test@gmail.com", password = "123456")
                )
            }
            val userTest = userDao.getUserByEmail("test@gmail.com")
            if (userTest != null && profileDao.getProfileByUserId(userTest.id) == null) {
                profileDao.saveProfile(
                    UserProfile(
                        userId       = userTest.id,
                        nama         = "User Test",
                        jenisKelamin = "Laki-laki",
                        tanggalLahir = "01-01-2000",
                        berat        = 110f,
                        tinggi       = 170f,
                        targetBerat  = 80f
                    )
                )
            }

            if (userDao.getUserByEmail("admin@nutrisee.com") == null) {
                userDao.register(
                    User(namaLengkap = "Admin", email = "admin@nutrisee.com", password = "admin123")
                )
            }
            val userAdmin = userDao.getUserByEmail("admin@nutrisee.com")
            if (userAdmin != null && profileDao.getProfileByUserId(userAdmin.id) == null) {
                profileDao.saveProfile(
                    UserProfile(
                        userId       = userAdmin.id,
                        nama         = "Admin",
                        jenisKelamin = "Laki-laki",
                        tanggalLahir = "01-01-1990",
                        berat        = 70f,
                        tinggi       = 175f,
                        targetBerat  = 65f
                    )
                )
            }
        }
    }
}