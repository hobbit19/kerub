package com.github.K0zka.kerub.data

import java.util.UUID
import com.github.K0zka.kerub.model.Project

public trait ProjectDao : CrudDao<Project, UUID>, ListableDao<Project, UUID> {
}